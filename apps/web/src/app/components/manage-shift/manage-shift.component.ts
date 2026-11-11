import {Component, inject, Input, OnDestroy, Output} from "@angular/core";
import {
  ActivityDto,
  ActivityEndpointService, EventEndpointService,
  LocationDto, LocationEndpointService, RoleEndpointService,
  ShiftDto,
  ShiftEndpointService, ShiftPlanDto
} from "../../../shiftservice-client";
import {InputSelectComponent, SelectOptions} from "../inputs/input-select/input-select.component";
import {
  BehaviorSubject, combineLatest,
  combineLatestWith,
  filter,
  map, Observable,
  of,
  shareReplay,
  startWith,
  Subject,
  Subscription,
  switchMap,
  take,
  tap
} from "rxjs";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {UserService} from "../../services/user/user.service";
import {InputTimeComponent, time} from "../inputs/input-time/input-time.component";
import {mapValue} from "../../util/value-maps";
import {AsyncPipe, DatePipe} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {DialogComponent} from "../dialog/dialog.component";
import {TooltipDirective} from "../../directives/tooltip.directive";
import {ManagePositionComponent} from "../manage-position/manage-position.component";
import {icons} from "../../util/icons";
import {InputNumberComponent} from "../inputs/input-number/input-number.component";
import {ToastService} from "../../services/toast/toast.service";
import {LockStatusPipe} from "../../pipes/lock-status.pipe";
import {descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";

export interface manageShiftParams {
  eventId: string;
  shift?: ShiftDto;
  suggestedDate?: Date;
  suggestedLocation?: LocationDto;
}

interface manageShiftData extends manageShiftParams {
  plans: ShiftPlanDto[];
  locations: LocationDto[];
  activities: ActivityDto[];
}

@Component({
  selector: "app-manage-shift",
  imports: [
    AsyncPipe,
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputDateComponent,
    InputTimeComponent,
    InputSelectComponent,
    InputButtonComponent,
    DialogComponent,
    TooltipDirective,
    ManagePositionComponent,
    InputNumberComponent,
    LockStatusPipe
  ],
  providers: [
    DatePipe
  ],
  templateUrl: "./manage-shift.component.html",
  styleUrl: "./manage-shift.component.scss"
})
export class ManageShiftComponent implements OnDestroy {

  @Output()
  public readonly shiftChanged = new Subject<ShiftDto>();

  @Output()
  public readonly navigateOut = new Subject<void>();

  protected readonly form;
  protected readonly icons = icons;

  protected readonly manageParams$ = new BehaviorSubject<undefined | manageShiftParams>(undefined);
  protected readonly manageData$: Observable<manageShiftData>;
  protected readonly userManageablePlans$: Observable<ShiftPlanDto[]>;
  protected readonly canManage$: Observable<boolean>;
  protected readonly positionManageData$;
  protected readonly activityOptions$: Observable<SelectOptions<ActivityDto>>;
  protected readonly locationOptions$: Observable<SelectOptions<LocationDto>>;
  protected readonly shiftPlanOptions$: Observable<SelectOptions<ShiftPlanDto>>;
  protected readonly suggestedActivities$ = new BehaviorSubject<ActivityDto[]>([]);
  protected readonly requestedEditMode$ = new BehaviorSubject<boolean>(false);

  protected showShiftDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _userService = inject(UserService);
  private readonly _shiftService = inject(ShiftEndpointService);
  private readonly _activityService = inject(ActivityEndpointService);
  private readonly _locationService = inject(LocationEndpointService);
  private readonly _roleService = inject(RoleEndpointService);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _datePipe = inject(DatePipe);
  private readonly _toastService = inject(ToastService);

  private readonly _disableLocationSubscription: Subscription;

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [nameLengthValidator, Validators.required]),
      description: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      startDate: this._fb.nonNullable.control<Date>(new Date()),
      startTime: this._fb.nonNullable.control<time>({hour: 0, minute: 0}, [Validators.required]),
      endDate: this._fb.nonNullable.control<Date>(new Date()),
      endTime: this._fb.nonNullable.control<time>({hour: 0, minute: 0}, [Validators.required]),
      plan: this._fb.control<ShiftPlanDto | null>(null, [Validators.required]),
      location: this._fb.control<LocationDto | null>(null),
      activity: this._fb.control<ActivityDto | null>(null),
      bonusPoints: this._fb.nonNullable.control<number>(0, [Validators.min(0)])
    });

    this._disableLocationSubscription = this.form.controls.activity.valueChanges.pipe(
      startWith(this.form.controls.activity.value)
    ).subscribe(activity => {
      const locationIsEnabled = this.form.controls.location.enabled;

      if(activity !== null) {
        this.form.controls.location.setValue(activity.location ?? null);
      }

      if (activity !== null && locationIsEnabled) {
        this.form.controls.location.disable();
      } else if (activity === null && !locationIsEnabled) {
        this.form.controls.location.enable();
      }
    });

    /**
     * All shift plans from this event that this user has planner-level access to
     */
    this.userManageablePlans$ = this.manageParams$.pipe(
      filter(data => data !== undefined),
      switchMap(data => this._eventService.getShiftPlansOverviewOfEvent(data.eventId)),
      switchMap(data => combineLatest(data.shiftPlans.map(plan => this._userService
        .canManagePlan$(plan.id).pipe(
          map(canManage => ({plan: plan, canManage}))
        )))
      ),
      map(plansWithPermissions => plansWithPermissions
        .filter(p => p.canManage)
        .map(p => p.plan)
      ),
      shareReplay()
    );

    /**
     * User can manage if creating a new shift & has access to some plans in this event,
     * or editing/viewing an existing shift & has access to the plan of that shift
     */
    this.canManage$ = this.manageParams$.pipe(
      filter(data => data !== undefined),
      combineLatestWith(this.userManageablePlans$.pipe(
        map(plans => plans.map(plan => plan.id))
      )),
      map(([params, manageable]) =>
        (params.shift === undefined && manageable.length > 0) ||
        (params.shift !== undefined && manageable.includes(params.shift.shiftPlan.id))
      )
    );

    /* locations of this event if manage access, or location of this existing shift */
    const availableLocations$ = this.manageParams$.pipe(
      filter(data => data !== undefined),
      switchMap(params => this.canManage$.pipe(
        switchMap(canManage => !canManage ?
          of(params.shift?.location ? [params.shift.location] : []) :
          this._locationService.getAllLocationsForEvent(params.eventId)),
      )),
      shareReplay()
    );

    /* activities of this event if manage access or activity of this existing shift */
    const availableActivities$ = this.manageParams$.pipe(
      filter(data => data !== undefined),
      switchMap(params => this.canManage$.pipe(
        switchMap(canManage => !canManage ?
          of(params.shift?.relatedActivity ? [params.shift.relatedActivity] : []) :
          this._activityService.getActivitiesForEvent(params.eventId)),
      )),
      shareReplay()
    );

    /**
     * Extended manage data including selectable plans, locations, activities
     */
    this.manageData$ = this.manageParams$.pipe(
      filter(data => data !== undefined),
      combineLatestWith(this.userManageablePlans$, availableLocations$, availableActivities$),
      map(([data, plans, locations, activities]) => ({
        ...data,
        plans,
        locations,
        activities
      })),
      shareReplay()
    );

    /* data containing select options for dropdowns */
    this.activityOptions$ = availableActivities$.pipe(
      map(activities => activities
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(activity => ({name: activity.name, value: activity})))
    );
    this.locationOptions$ = availableLocations$.pipe(
      map(locations => locations
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(location => ({name: location.name, value: location})))
    );
    this.shiftPlanOptions$ = this.manageData$.pipe(
      map(data => data.plans
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(plan => ({name: plan.name, value: plan})))
    );

    /**
     * Manage data for each position slot in this shift, plus an additional entry for creating a new slot
     */
    this.positionManageData$ = this.manageData$.pipe(
      combineLatestWith(this.manageParams$.pipe(
        filter(params => params !== undefined),
        switchMap(params => params.shift === undefined ? of([]) : this._roleService.getRoles(params.shift.shiftPlan.id))
      ), this.canManage$),
      map(([data, roles, canManage]) => {
        if(data === undefined || data.shift === undefined) {
          return [];
        }
        const shift = data.shift;

        const newSlotManageData = {
          shift,
          position: undefined,
          availableRoles: roles
            .sort((a, b) => a.name.localeCompare(b.name))
            .map(role => ({name: role.name, value: role}))
        };
        const existingSlotManageData = shift.positionSlots.map(slot => ({
            shift,
            position: slot,
            availableRoles: roles
              .sort((a, b) => a.name.localeCompare(b.name))
              .map(role => ({name: role.name, value: role}))
          })
        );
        return [ ...(canManage? [newSlotManageData] : []), ...existingSlotManageData];
      })
    );
  }

  /**
   * Current mode: create / edit / view
   * - create: no shift provided, user can manage
   * - edit: shift provided, user can manage plan of shift, edit requested
   * - view: otherwise
   */
  public get mode$(){
    return this.manageParams$.pipe(
      combineLatestWith(this.canManage$, this.requestedEditMode$),
      map(([data, canManage, requestedEdit]) => {
        if(data?.shift === undefined && canManage) {
          return "create";
        }
        if(requestedEdit && canManage && data !== undefined) {
          return "edit";
        }
        return "view";
      })
    );
  }

  public get title$(){
    return this.mode$.pipe(
      combineLatestWith(this.manageParams$),
      map(([mode, data]) => {
        switch(mode) {
          case "create":
            return "Create Shift";
          case "edit":
            return `${data?.shift?.name ?? ""}`;
          case "view":
            return `${data?.shift?.name ?? ""}`;
        }
        return "";
      })
    );
  }

  @Input()
  public set manageData(value: manageShiftParams) {
    this.manageParams$.next(value);

    this.canManage$.pipe(
      switchMap(canManage => canManage ?
        this._activityService.suggestActivitiesForShift(value.eventId, {
          timeFilter: value.shift !== undefined ? {
            startTime: value.shift?.startTime,
            endTime: value.shift?.endTime,
            toleranceInMinutes: 60 * 5
          } : value.suggestedDate ? {
            startTime: new Date(value.suggestedDate.getTime()).toISOString(),
            endTime: new Date(value.suggestedDate.getTime() + 1000 * 60 * 60 * 3).toISOString(),
            toleranceInMinutes: 60 * 5
          } : undefined
        }) : of([])
      )
    ).subscribe(suggestions => {
      this.suggestedActivities$.next(suggestions);
    });

    if (value.shift !== undefined) {
      this.form.controls.plan.disable();
      this.form.setValue({
        name: value.shift.name,
        description: value.shift.longDescription ?? "",
        startDate: new Date(value.shift.startTime),
        startTime: mapValue.datetimeStringAsLocalTime(value.shift.startTime),
        endDate: new Date(value.shift.endTime),
        endTime: mapValue.datetimeStringAsLocalTime(value.shift.endTime),
        location: value.shift.location ?? null,
        plan: value.shift.shiftPlan,
        activity: value.shift.relatedActivity ?? null,
        bonusPoints: value.shift.bonusRewardPoints
      });
    } else {
      this.form.controls.plan.enable();
      this.form.setValue({
        name: "",
        description: "",
        startDate: value.suggestedDate ?? new Date(),
        startTime: value.suggestedDate ? mapValue.datetimeStringAsLocalTime(value.suggestedDate.toISOString()) : {hour: 10, minute: 0},
        endDate: value.suggestedDate ? new Date(value.suggestedDate.getTime() + 1000 * 60 * 60 * 2) : new Date(),
        endTime: value.suggestedDate ?
          mapValue.datetimeStringAsLocalTime(new Date(value.suggestedDate.getTime() + 1000 * 60 * 60 * 2).toISOString()) :
          {hour: 12, minute: 0},
        location: value.suggestedLocation ?? null,
        activity: null,
        plan: null,
        bonusPoints: 0
      });
    }
  }

  ngOnDestroy() {
    this._disableLocationSubscription.unsubscribe();
  }

  /**
   * refresh shift dto and write back to the manage params
   * @param shiftId
   * @protected
   */
  protected refreshShift(shiftId: string){
    this.manageParams$.pipe(
      take(1),
      switchMap(data => this._shiftService.getShiftDetails(shiftId).pipe(
        tap(shift => this.shiftChanged.next(shift.shift)),
        map(shift => ({...data, shift: shift.shift} as manageShiftParams))
      ))
    ).subscribe(params =>this.manageParams$.next(params));
  }

  /**
   * Link activity and location dto to the form based on the provided activity
   * @param activity
   * @param manageData
   * @protected
   */
  protected linkAndFill(activity: ActivityDto, manageData: manageShiftData) {
    const matchingActivity = manageData.activities
      .find(o => o.id === activity.id);
    this.form.controls.activity.setValue(matchingActivity ?? null);

    const matchingLocation = manageData.locations
      .find(o => o.id === activity.location?.id);
    this.form.controls.location.setValue(matchingLocation ?? null);

    if(matchingActivity !== undefined) {
      this.form.controls.startTime.setValue(mapValue.datetimeStringAsLocalTime(activity.startTime));
      this.form.controls.startDate.setValue(new Date(activity.startTime));
      this.form.controls.endTime.setValue(mapValue.datetimeStringAsLocalTime(activity.endTime));
      this.form.controls.endDate.setValue(new Date(activity.endTime));

      this.form.controls.description.setValue(`Shift for activity: ${activity.name}`);
      this.form.controls.name.setValue(`${activity.name} Shift`);
    }
  }

  /**
   * Create a new shift based on the form data
   * @protected
   */
  protected create() {
    this.form.markAllAsTouched();

    if(this.form.valid) {

      const start = mapValue.combineDateAndLocalTime(this.form.controls.startDate.value, this.form.controls.startTime.value);
      const end = mapValue.combineDateAndLocalTime(this.form.controls.endDate.value, this.form.controls.endTime.value);

      if(start === undefined || end === undefined) {
        throw new Error("Start and end date must be defined");
      }

      const planId = this.form.controls.plan.value?.id ?? undefined;
      if(planId === undefined) {
        throw new Error("Shift plan must be defined");
      }

      this._shiftService.createShift(planId, {
        name: this.form.controls.name.value,
        longDescription: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        /* location only if not activity set*/
        locationId: this.form.controls.activity.value === null ? (this.form.controls.location.value?.id ?? undefined) : undefined,
        activityId: this.form.controls.activity.value?.id ?? undefined,
        bonusRewardPoints: this.form.controls.bonusPoints.value,
      }).pipe(
        this._toastService.tapCreating("Shift", item => item.name)
      ).subscribe(shift => {
        this.requestedEditMode$.next(false);
        this.refreshShift(shift.id);
      });
    } else {
      this._toastService.showError("Invalid Shift", "Please provide valid shift details.");
    }
  }

  /**
   * Update an existing shift based on the form data
   * @param shift
   * @protected
   */
  protected update(shift: ShiftDto) {
    this.form.markAllAsTouched();

    if(this.form.valid) {

      const start = mapValue.combineDateAndLocalTime(this.form.controls.startDate.value, this.form.controls.startTime.value);
      const end = mapValue.combineDateAndLocalTime(this.form.controls.endDate.value, this.form.controls.endTime.value);

      if(start === undefined || end === undefined) {
        throw new Error("Start and end date must be defined");
      }

      this._shiftService.updateShift(shift.id, {
        name: this.form.controls.name.value,
        longDescription: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        /* location only if no activity set*/
        locationId: this.form.controls.activity.value === null ? (this.form.controls.location.value?.id ?? undefined) : undefined,
        activityId: this.form.controls.activity.value?.id ?? undefined,
        bonusRewardPoints: this.form.controls.bonusPoints.value
      }).pipe(
        this._toastService.tapSaving("Shift", item => item.name)
      ).subscribe(updatedShift => {
        this.requestedEditMode$.next(false);
        this.refreshShift(updatedShift.id);
      });
    }else {
      this._toastService.showError("Invalid Shift", "Please provide valid shift details.");
    }
  }

  /**
   * Delete an existing shift
   * @param shift
   * @protected
   */
  protected delete(shift: ShiftDto) {
    this._shiftService.deleteShift(shift.id).pipe(
      this._toastService.tapDeleting("Shift", () => shift.name)
    ).subscribe(() => {
      this.shiftChanged.next(shift);
      this.navigateOut.next();
    });
  }

  /**
   * Comparator function for dropdowns based on ID
   * @param a
   * @param b
   * @protected
   */
  protected idComparatorFn(a: { id: string } | null, b: { id: string } | null): boolean {
    return a?.id === b?.id || (a === null && b === null);
  }

  protected getDateRange(shift: ShiftDto): string {
    const start = new Date(shift.startTime);
    const end = new Date(shift.endTime);

    return mapValue.dateRangeToDateTimeString(start, end, this._datePipe);
  }
}
