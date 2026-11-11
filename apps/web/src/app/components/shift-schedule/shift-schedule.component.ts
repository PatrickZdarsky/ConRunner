import {Component, Input} from "@angular/core";
import {RouterLink} from "@angular/router";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {TooltipDirective} from "../../directives/tooltip.directive";
import {EventDto, PositionSlotDto, ShiftDto, ShiftPlanDto} from "../../../shiftservice-client";
import {BehaviorSubject} from "rxjs";
import {AsyncPipe, DatePipe, NgClass} from "@angular/common";
import {icons} from "../../util/icons";

type groupedShifts = Map<number, shiftWithOrigin[]>;

export interface shiftWithOrigin {
  shift: ShiftDto;
  originEvent: EventDto;
  originPlan: ShiftPlanDto;
}

@Component({
  selector: "app-shift-schedule",
  imports: [
    RouterLink,
    FaIconComponent,
    TooltipDirective,
    AsyncPipe,
    DatePipe,
    NgClass
  ],
  standalone: true,
  templateUrl: "./shift-schedule.component.html",
  styleUrl: "./shift-schedule.component.scss"
})
export class ShiftScheduleComponent {

  @Input()
  public showShiftOrigin = false;

  @Input()
  public showShiftDescription = false;

  @Input()
  public showShiftPersonalDetails = false;

  protected readonly shifts$ = new BehaviorSubject<groupedShifts>(new Map());
  protected readonly icons = icons;

  @Input()
  public set shifts(shifts: shiftWithOrigin[]) {
    this.shifts$.next(this.groupShiftsByLocalDate(shifts));
  }

  protected groupShiftsByLocalDate(shifts: shiftWithOrigin[]): groupedShifts {
    const map: groupedShifts = new Map();
    for (const shift of shifts) {
      const localDate = new Date(shift.shift.startTime);
      localDate.setHours(0, 0, 0, 0);
      const key = localDate.getTime();
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)?.push(shift);
    }

    return map;
  }

  protected getAssignedPositionSlots(shift: ShiftDto) {
    return shift.positionSlots.filter(slot =>
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignedUp
    );
  }

  /**
   * If no position as signed up, it is only requested to join
   * @param shift
   * @protected
   */
  protected isRequested(shift: ShiftDto) {
    return shift.positionSlots
      .filter(slot => slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignedUp)
      .length === 0;
  }

}
