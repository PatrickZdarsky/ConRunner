import {Component, inject, Input, OnDestroy, Output} from "@angular/core";
import {
  BehaviorSubject, catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  map, merge, of,
  shareReplay,
  Subject,
  Subscription,
  switchMap, take, tap, withLatestFrom,
} from "rxjs";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import { icons } from "../../../util/icons";
import {ShiftPlanDto, UserEventDto, UserEventEndpointService} from "../../../../shiftservice-client";
import {AsyncPipe} from "@angular/common";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../inputs/input-multitoggle/input-multi-toggle.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {InputToggleComponent} from "../../inputs/input-toggle/input-toggle.component";
import {InputButtonComponent} from "../../inputs/input-button/input-button.component";
import {InputSelectComponent} from "../../inputs/input-select/input-select.component";
import {ToastService} from "../../../services/toast/toast.service";

@Component({
  selector: "app-manage-volunteer-plan",
  imports: [
    AsyncPipe,
    InputMultiToggleComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputToggleComponent,
    InputButtonComponent,
    InputSelectComponent
  ],
  templateUrl: "./manage-volunteer-plan.component.html",
  styleUrl: "./manage-volunteer-plan.component.scss"
})
export class ManageVolunteerPlanComponent implements OnDestroy {

  @Output()
  public planChanged = new Subject<void>();

  protected readonly icons = icons;
  protected readonly levelOptions: MultiToggleOptions<"volunteer" | "planner"> = [
    {value: "volunteer", name: "Volunteer"},
    {value: "planner", name: "Planner"}
  ];
  protected readonly form;
  protected readonly manageData$ = new BehaviorSubject<{
    volunteer: UserEventDto; plan: ShiftPlanDto | undefined; plans: ShiftPlanDto[];
  } | undefined>(undefined);
  protected readonly newPlanOptions$ = this.manageData$.pipe(
    map(data => data?.plans
      ?.sort((a,b) => a.name.localeCompare(b.name))
      .map(plan => ({
        value: plan,
        name: plan.name
    })) ?? []),
    shareReplay()
  );

  private readonly _fb = inject(FormBuilder);
  private readonly _toastService = inject(ToastService);
  private readonly _userEventService = inject(UserEventEndpointService);
  private _saveSubscription?: Subscription;

  constructor() {
    this.form = this._fb.group({
      plan: this._fb.nonNullable.control<ShiftPlanDto | undefined>(undefined, [Validators.required]),
      accessLevel: this._fb.nonNullable.control<"planner" | "volunteer">("volunteer", [Validators.required]),
      locked: this._fb.nonNullable.control<boolean>(false, [Validators.required])
    });
  }

  @Input()
  public set manageData(data: { volunteer: UserEventDto; plan: ShiftPlanDto | undefined; plans: ShiftPlanDto[] } | undefined) {
    this._saveSubscription?.unsubscribe();
    this.manageData$.next(data);

    this.form.reset();

    if(data?.plan !== undefined) {
      const isLocked = data.volunteer.lockedPlans.includes(data.plan.id);
      const isVolunteer = data.volunteer.volunteeringPlans.includes(data.plan.id);
      const isPlanner = data.volunteer.planningPlans.includes(data.plan.id);

      if(!isVolunteer && !isPlanner || isPlanner && !isVolunteer) {
        throw new Error("invalid volunteer state");
      }

      this.form.controls.locked.setValue(isLocked);
      this.form.controls.accessLevel.setValue(isPlanner ? "planner" : "volunteer");
      this.form.controls.plan.setValue(data.plan);

      this.listenFormChanges();
    } else {
      this.form.controls.plan.setValue(data?.plans?.at(0) ?? undefined);
    }

    this.form.markAsPristine();
    this.form.updateValueAndValidity();
  }

  ngOnDestroy(): void {
    this._saveSubscription?.unsubscribe();
  }

  protected listenFormChanges() {
    this._saveSubscription?.unsubscribe();

    const lockedChanges$ = this.form.controls.locked.valueChanges.pipe(
      debounceTime(100),
      distinctUntilChanged(),
      withLatestFrom(this.manageData$.pipe(
        filter((data): data is { volunteer: UserEventDto; plan: ShiftPlanDto; plans: ShiftPlanDto[] } =>
          data !== undefined && data.plan !== undefined
      ))),
      switchMap(([locked, data]) => {
        const isLocked = data.volunteer.lockedPlans.includes(data.plan.id);

        /* lock plan */
        if(locked && !isLocked) {
          return this._userEventService.lockUserInPlan(data.volunteer.volunteer.id, {
            shiftPlanIds: [data.plan.id]
          }).pipe(
            this._toastService.tapSaving("Access Lock", () => data.plan.name)
          );
        } /* unlock plan */ else if(!locked && isLocked) {
          return this._userEventService.unlockUserInPlan(data.volunteer.volunteer.id, {
            shiftPlanIds: [data.plan.id]
          }).pipe(
            this._toastService.tapDeleting("Access Lock", () => data.plan.name)
          );
        } else {
          return of(undefined);
        }
      }),
    );

    const accessLevelChanges$ = this.form.controls.accessLevel.valueChanges.pipe(
      debounceTime(100),
      distinctUntilChanged(),
      withLatestFrom(this.manageData$.pipe(
        filter((data): data is { volunteer: UserEventDto; plan: ShiftPlanDto; plans: ShiftPlanDto[] } =>
          data !== undefined && data.plan !== undefined
      ))),
      switchMap(([level, data]) => {
        const isPlanner = data.volunteer.planningPlans.includes(data.plan.id);

        /* grant planning access */
        if(level === "planner" && !isPlanner) {
          const newPlanningPlans = [...data.volunteer.planningPlans, data.plan.id];
          return this._userEventService.updateUserPlans(data.volunteer.volunteer.id, {
            planningPlans: newPlanningPlans,
            volunteeringPlans: data.volunteer.volunteeringPlans
          }).pipe(
            this._toastService.tapSaving("Planner Access", () => data.plan.name)
          );
        } /* revoke planning access */ else if(level === "volunteer" && isPlanner) {
          const newPlanningPlans = data.volunteer.planningPlans.filter(id => id !== data.plan.id);
          return this._userEventService.updateUserPlans(data.volunteer.volunteer.id, {
            planningPlans: newPlanningPlans,
            volunteeringPlans: data.volunteer.volunteeringPlans
          }).pipe(
            this._toastService.tapDeleting("Planner Access", () => data.plan.name)
          );
        } else {
          return of(undefined);
        }
      }),
    );

    this._saveSubscription = merge(lockedChanges$, accessLevelChanges$).pipe(

      /* reset component on error */
      catchError(e => this.manageData$.pipe(
          take(1),
          tap(data => this.manageData = data),
          tap(() => {
            throw e;
          })
        )),
      filter(data => data !== undefined)
    ).subscribe(() => {
      this.planChanged.next();
    });
  }

  protected addAccess(volunteer: UserEventDto) {
    this.form.markAllAsTouched();

    if(this.form.invalid) {
      this._toastService.showError("Invalid Data", "Please provide valid plan access details.");
      return;
    }

    const plan = this.form.controls.plan.value;
    const accessLevel = this.form.controls.accessLevel.value;

    if(plan === undefined) {
      this._toastService.showError("Invalid Data", "Please select a valid plan.");
      return;
    }
    const newPlan = plan;

    const newPlanningPlans = [...volunteer.planningPlans];
    const newVolunteeringPlans = [...volunteer.volunteeringPlans];

    if(accessLevel === "planner") {
      newVolunteeringPlans.push(plan.id);
      newPlanningPlans.push(plan.id);
    } else {
      newVolunteeringPlans.push(plan.id);
    }

    this._userEventService.updateUserPlans(volunteer.volunteer.id, {
      planningPlans: newPlanningPlans,
      volunteeringPlans: newVolunteeringPlans,
    }).pipe(
      this._toastService.tapSaving("Plan Access", () => newPlan.name)
    ).subscribe(() => {
      this.planChanged.next();
      this.form.reset();
    });
  }

  protected idComparator(a: ShiftPlanDto | null, b: ShiftPlanDto | null) {
    return a?.id === b?.id;
  }

  protected revokeAccess(volunteer: UserEventDto, plan: ShiftPlanDto) {
    const newPlanningPlans = volunteer.planningPlans.filter(id => id !== plan.id);
    const newVolunteeringPlans = volunteer.volunteeringPlans.filter(id => id !== plan.id);

    this._userEventService.updateUserPlans(volunteer.volunteer.id, {
      planningPlans: newPlanningPlans,
      volunteeringPlans: newVolunteeringPlans,
    }).pipe(
      this._toastService.tapDeleting("Plan Access", () => plan.name)
    ).subscribe(() => {
      this.planChanged.next();
    });
  }

  protected resetAssignments(volunteer: UserEventDto, plan: ShiftPlanDto) {
    this._userEventService.resetUserInPlan(volunteer.volunteer.id, {
      shiftPlanIds: [plan.id]
    }).pipe(
      this._toastService.tapDeleting("User Assignments", () => plan.name)
    ).subscribe(() => {
      this.planChanged.next();
    });
  }
}
