import {Component, inject, Input} from "@angular/core";
import {AsyncPipe, DatePipe} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {
  EventShiftPlansOverviewDto,
  TimeConstraintCreateDto,
  TimeConstraintDto,
  TimeConstraintEndpointService
} from "../../../shiftservice-client";
import {BehaviorSubject, of, switchMap} from "rxjs";
import {FormBuilder} from "@angular/forms";
import {DialogAddEmergencyComponent} from "../dialog-add-emergency/dialog-add-emergency.component";
import {addUnavailabilityInput, DialogAddUnavailabilityComponent} from "../dialog-add-unavailability/dialog-add-unavailability.component";
import {TooltipDirective} from "../../directives/tooltip.directive";

@Component({
  selector: "app-manage-unavailability",
  imports: [
    AsyncPipe,
    DatePipe,
    FaIconComponent,
    InputButtonComponent,
    DialogAddEmergencyComponent,
    DialogAddUnavailabilityComponent,
    TooltipDirective
  ],
  standalone: true,
  templateUrl: "./manage-unavailability.component.html",
  styleUrl: "./manage-unavailability.component.scss"
})
export class ManageUnavailabilityComponent {

  protected event$ = new BehaviorSubject<EventShiftPlansOverviewDto | undefined>(undefined);
  protected timeConstraints$ = this.event$.pipe(
    switchMap((event) => event === undefined ?
      of([]) : this._timeConstraintService.getTimeConstraints(event.eventOverview.id)
    )
  );

  protected readonly form;
  protected readonly icons = icons;
  protected showUnavailabilityDialog = false;
  protected showEmergencyDialog = false;

  private readonly _timeConstraintService = inject(TimeConstraintEndpointService);
  private readonly _toastService = inject(ToastService);
  private readonly _fb = inject(FormBuilder);

  constructor() {
    this.form = this._fb.group({
      unavailabilityFromEventStart: this._fb.nonNullable.control<boolean>(false),
      unavailabilityUntilEventEnd: this._fb.nonNullable.control<boolean>(false),
      unavailabilityFrom: this._fb.nonNullable.control<Date>(new Date()),
      unavailabilityUntil: this._fb.nonNullable.control<Date>(new Date()),
    });
  }

  @Input()
  public set event(event: EventShiftPlansOverviewDto | undefined) {
    this.event$.next(event);
  }

  protected getUnavailabilityDayLength(constraint: TimeConstraintDto): string {
    const from = new Date(constraint.from);
    const to = new Date(constraint.to);
    const diffTime = Math.abs(to.getTime() - from.getTime());
    const days = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return `${days} day${days > 1 ? "s" : ""}`;
  }


  protected unavailabilitySubmitted(input: addUnavailabilityInput | undefined, event: EventShiftPlansOverviewDto){
    this.showUnavailabilityDialog = false;
    if(input === undefined) {
      return;
    }

    this._timeConstraintService.createTimeConstraint(event.eventOverview.id, {
      from: input.start ? input.start.toISOString() : event.eventOverview.startTime,
      to: input.end ? input.end.toISOString() : event.eventOverview.endTime,
      type: TimeConstraintCreateDto.TypeEnum.Unavailable
    }).pipe(
      this._toastService.tapCreating("Absence")
    ).subscribe(() => {
      this.timeConstraints$ = this._timeConstraintService.getTimeConstraints(event.eventOverview.id);
    });
  }

  protected emergencySubmitted(emergencyDate: Date | undefined, event: EventShiftPlansOverviewDto) {
    this.showEmergencyDialog = false;
    if (emergencyDate === undefined) {
      return;
    }

    this._timeConstraintService.createTimeConstraint(event.eventOverview.id, {
      from: emergencyDate.toISOString(),
      to: new Date(emergencyDate.getTime() + 24 * 60 * 60 * 1000).toISOString(),
      type: TimeConstraintCreateDto.TypeEnum.Emergency
    }).pipe(
      this._toastService.tapCreating("Emergency")
    ).subscribe(() => {
      this.timeConstraints$ = this._timeConstraintService.getTimeConstraints(event.eventOverview.id);
    });
  }

  protected removeUnavailability(constraint: TimeConstraintDto, event: EventShiftPlansOverviewDto) {
    this._timeConstraintService.deleteTimeConstraint(event.eventOverview.id, constraint.id).subscribe(() => {
      this.timeConstraints$ = this._timeConstraintService.getTimeConstraints(event.eventOverview.id);
    });
  }
}
