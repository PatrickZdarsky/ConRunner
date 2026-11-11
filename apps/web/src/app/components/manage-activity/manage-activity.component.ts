import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {BehaviorSubject, filter, map, Observable, switchMap} from "rxjs";
import {DialogComponent} from "../dialog/dialog.component";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {ActivityDto, ActivityEndpointService, LocationDto, LocationEndpointService} from "../../../shiftservice-client";
import {AsyncPipe, DatePipe} from "@angular/common";
import {InputSelectComponent, SelectOptions} from "../inputs/input-select/input-select.component";
import {InputTimeComponent, time} from "../inputs/input-time/input-time.component";
import {mapValue} from "../../util/value-maps";
import {UserService} from "../../services/user/user.service";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";

export interface manageActivityParams {
  eventId: string;
  activity?: ActivityDto;
  suggestedDate?: Date;
  suggestedLocation?: LocationDto;
}

interface manageActivityData extends manageActivityParams {
  locations: LocationDto[];
}

@Component({
  selector: "app-manage-activity",
  imports: [
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    FaIconComponent,
    InputDateComponent,
    InputButtonComponent,
    DialogComponent,
    AsyncPipe,
    InputSelectComponent,
    InputTimeComponent,
    DatePipe
  ],
  templateUrl: "./manage-activity.component.html",
  styleUrl: "./manage-activity.component.scss"
})
export class ManageActivityComponent {

  @Output()
  public readonly activityChanged = new EventEmitter<ActivityDto>();

  public readonly form;
  protected readonly icons = icons;

  protected readonly manageParams$ = new BehaviorSubject<undefined | manageActivityParams>(undefined);
  protected readonly mangeData$: Observable<manageActivityData>;
  protected readonly locationOptions$: Observable<SelectOptions<LocationDto>>;

  protected showActivityDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _activityService = inject(ActivityEndpointService);
  private readonly _locationService = inject(LocationEndpointService);
  private readonly _userService = inject(UserService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [nameLengthValidator, Validators.required]),
      description: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      startDate: this._fb.nonNullable.control<Date>(new Date()),
      startTime: this._fb.nonNullable.control<time>({hour: 0, minute: 0}, [Validators.required]),
      endDate: this._fb.nonNullable.control<Date>(new Date()),
      endTime: this._fb.nonNullable.control<time>({hour: 0, minute: 0}, [Validators.required]),
      location: this._fb.control<LocationDto | null>(null)
    });

    this.mangeData$ = this.manageParams$.pipe(
      filter((params): params is manageActivityParams => params !== undefined),
      switchMap(params =>
        this._locationService.getAllLocationsForEvent(params.eventId).pipe(
          map(locations => ({
            ...params,
            locations: locations
          }))
        )
      )
    );

    this.locationOptions$ = this.mangeData$.pipe(
      map(data => data.locations.map(loc => ({
        name: loc.name,
        value: loc
      })))
    );
  }

  public get canManage$() {
    return this._userService.isPlatformAdmin$;
  }

  @Input({required: true})
  public set manageData(value: manageActivityParams) {
    this.manageParams$.next(value);

    if(value.activity !== undefined) {
      this.form.setValue({
        name: value.activity.name,
        description: value.activity.description ?? "",
        startDate: new Date(value.activity.startTime),
        startTime: mapValue.datetimeStringAsLocalTime(value.activity.startTime),
        endDate: new Date(value.activity.endTime),
        endTime: mapValue.datetimeStringAsLocalTime(value.activity.endTime),
        location: value.activity.location ?? null
      });
    } else {
      this.form.setValue({
        name: "",
        description: "",
        startDate: value.suggestedDate ?? new Date(),
        startTime: value.suggestedDate ? mapValue.datetimeStringAsLocalTime(value.suggestedDate.toISOString()) : {hour: 10, minute: 0},
        endDate: value.suggestedDate ? new Date(value.suggestedDate.getTime() + 1000 * 60 * 60 * 2) : new Date(),
        endTime: value.suggestedDate ?
          mapValue.datetimeStringAsLocalTime(new Date(value.suggestedDate.getTime() + 1000 * 60 * 60 * 2).toISOString()) :
          {hour: 12, minute: 0},
        location: value.suggestedLocation ?? null
      });
    }
  }

  protected create(eventId: string){
    this.form.markAllAsTouched();

    if(this.form.valid) {

      const start = mapValue.combineDateAndLocalTime(this.form.controls.startDate.value, this.form.controls.startTime.value);
      const end = mapValue.combineDateAndLocalTime(this.form.controls.endDate.value, this.form.controls.endTime.value);

      if(start === undefined || end === undefined) {
        throw new Error("Start and end date must be defined");
      }

      this._activityService.createActivity(eventId, {
        name: this.form.controls.name.value,
        description: this.form.controls.description.value,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        locationId: this.form.controls.location.value?.id ?? undefined
      }).pipe(
        this._toastService.tapCreating("Activity", item => item.name)
      ).subscribe(activity => {
        this.activityChanged.emit(activity);
      });
    }else {
      this._toastService.showError("Invalid Activity", "Please provide valid activity details.");
    }
  }

  protected update(activity: ActivityDto){
    this.form.markAllAsTouched();

    if(this.form.valid) {

      const start = mapValue.combineDateAndLocalTime(this.form.controls.startDate.value, this.form.controls.startTime.value);
      const end = mapValue.combineDateAndLocalTime(this.form.controls.endDate.value, this.form.controls.endTime.value);

      if(start === undefined || end === undefined) {
        throw new Error("Start and end date must be defined");
      }

      this._activityService.updateActivity(activity.id, {
        name: this.form.controls.name.value,
        description: this.form.controls.description.value,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        locationId: this.form.controls.location.value?.id ?? undefined
      }).pipe(
        this._toastService.tapSaving("Activity", item => item.name)
      ).subscribe(updated => {
        this.activityChanged.emit(updated);
      });
    }else {
      this._toastService.showError("Invalid Activity", "Please provide valid activity details.");
    }
  }

  protected delete(activity: ActivityDto) {
    this._activityService.deleteActivity(activity.id).pipe(
      this._toastService.tapDeleting("Activity")
    ).subscribe(() => {
      this.activityChanged.emit(activity);
    });
  }

  protected idComparatorFn(a: LocationDto | null, b: LocationDto | null): boolean {
    return a?.id === b?.id;
  }

}
