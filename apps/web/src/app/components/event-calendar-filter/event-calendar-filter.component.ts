import {Component, inject, Input} from "@angular/core";
import {faEye} from "@fortawesome/free-solid-svg-icons";
import {InputSelectComponent, SelectOptions} from "../inputs/input-select/input-select.component";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {InputMultiselectComponent} from "../inputs/input-multiselect/input-multiselect.component";
import {DecimalPipe, NgClass} from "@angular/common";
import {ScheduleStatisticsDto} from "../../../shiftservice-client";

type ShiftRelevancesEnum = "MY_SHIFTS" | "SIGNUP_POSSIBLE";

export enum ShiftCalendarViewMode {
  Calendar = "calendar",
  Table = "table"
}

@Component({
  selector: "app-event-calendar-filter",
  imports: [
    InputTextComponent,
    TypedFormControlDirective,
    ReactiveFormsModule,
    InputDateComponent,
    InputSelectComponent,
    InputMultiselectComponent,
    NgClass,
    DecimalPipe
  ],
  standalone: true,
  templateUrl: "./event-calendar-filter.component.html",
  styleUrl: "./event-calendar-filter.component.scss"
})
export class EventCalendarFilterComponent {

  @Input()
  public showFilters = true;

  @Input()
  public showShiftFilterForm = true;

  @Input()
  public statistics?: ScheduleStatisticsDto;

  @Input()
  public rolesOptions: SelectOptions<string> = [];

  @Input()
  public locationsOptions: SelectOptions<string> = [];

  @Input()
  public plansOptions: SelectOptions<string> = [];

  @Input()
  public statisticsMode: "admin" | "planner" | "volunteer" = "volunteer";

  public readonly availabilityOptions: SelectOptions<ShiftRelevancesEnum> = [
    {name: "Your Shifts", value: "MY_SHIFTS"},
    {name: "Signup Possible", value: "SIGNUP_POSSIBLE"}
  ];
  public readonly viewModeOptions: SelectOptions<ShiftCalendarViewMode> = [
    {name: "Calendar", value: ShiftCalendarViewMode.Calendar},
    {name: "Table", value: ShiftCalendarViewMode.Table}
  ];
  public readonly searchForm;
  public readonly viewForm;

  protected iconView = faEye;

  private readonly _fb = inject(FormBuilder);

  constructor() {

    /*
    Selecting all filter options does not mean no filtering is applied:
    [ ] -> No filters selected; all values shown
    [ some items ] -> Some filters selected; only matching values shown
    [ all items ] -> All filters selected; only matching values shown
    */

    this.searchForm = this._fb.group({
      shiftName: this._fb.nonNullable.control<string>(""),
      rolesList: this._fb.nonNullable.control<string[]>([]),
      plansList: this._fb.nonNullable.control<string[]>([]),
      locationsList: this._fb.nonNullable.control<string[]>([]),
      relevanceList: this._fb.nonNullable.control<ShiftRelevancesEnum[]>([])
    });
    this.viewForm = this._fb.group({
      date: this._fb.nonNullable.control<Date>(new Date()),
      viewMode: this._fb.nonNullable.control<ShiftCalendarViewMode>(ShiftCalendarViewMode.Calendar),
    });
  }

}
