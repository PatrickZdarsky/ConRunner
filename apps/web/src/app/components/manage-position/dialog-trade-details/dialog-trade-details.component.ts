import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {
  PositionSlotContextDto,
  PositionSlotTradeEndpointService, ShiftContextDto,
  TradeInfoDto,
  VolunteerDto
} from "../../../../shiftservice-client";
import {BehaviorSubject, combineLatestWith, filter, map, Observable, of, switchMap} from "rxjs";
import {UserService} from "../../../services/user/user.service";
import {AsyncPipe, DatePipe, SlicePipe} from "@angular/common";
import {DialogComponent, dialogResult} from "../../dialog/dialog.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {icons} from "../../../util/icons";
import {ToastService} from "../../../services/toast/toast.service";
import {mapValue} from "../../../util/value-maps";
import {RouterLink} from "@angular/router";

interface mappedInfo {
  tradeId: string;
  isOwnRequest: boolean;
  ownRewardPoints: number;
  otherRewardPoints: number;
  ownVolunteer: VolunteerDto;
  otherVolunteer: VolunteerDto;
  ownSlot: PositionSlotContextDto;
  otherSlot: PositionSlotContextDto;
  ownShift: ShiftContextDto;
  otherShift: ShiftContextDto;
}

@Component({
  selector: "app-dialog-trade-details",
  imports: [
    AsyncPipe,
    SlicePipe,
    DialogComponent,
    FaIconComponent,
    DatePipe,
    RouterLink
  ],
  templateUrl: "./dialog-trade-details.component.html",
  styleUrl: "./dialog-trade-details.component.scss"
})
export class DialogTradeDetailsComponent {

  @Output()
  tradeChanged = new EventEmitter<void>();

  @Output()
  dialogClosed = new EventEmitter<void>();

  protected icons = icons;

  protected tradeInfo$ = new BehaviorSubject<TradeInfoDto | undefined>(undefined);
  protected mappedInfo$: Observable<mappedInfo | undefined>;

  private readonly _userService = inject(UserService);
  private readonly _tradeService = inject(PositionSlotTradeEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.mappedInfo$ = this.tradeInfo$.pipe(
      switchMap(_info => _info === undefined ? of(undefined) : of(_info).pipe(
        combineLatestWith(this._userService.userProfile$.pipe(
          filter(user => user !== null)
        )),
        map(([info, user]) => {
          const isOwnRequest = info.offeringAssignment.assignment.assignedVolunteer.id === user.account.volunteer.id;

          return {
            tradeId: info.id,
            isOwnRequest,
            ownRewardPoints: isOwnRequest ? info.offeredPositionSlotRewardPoints : info.requestedPositionSlotRewardPoints,
            otherRewardPoints: isOwnRequest ? info.requestedPositionSlotRewardPoints : info.offeredPositionSlotRewardPoints,
            ownVolunteer: isOwnRequest ? info.offeringAssignment.assignment.assignedVolunteer :
              info.requestedAssignment.assignment.assignedVolunteer,
            otherVolunteer: isOwnRequest ? info.requestedAssignment.assignment.assignedVolunteer :
              info.offeringAssignment.assignment.assignedVolunteer,
            ownSlot: isOwnRequest ? info.offeringAssignment.positionSlotContext : info.requestedAssignment.positionSlotContext,
            otherSlot: isOwnRequest ? info.requestedAssignment.positionSlotContext : info.offeringAssignment.positionSlotContext,
            ownShift: isOwnRequest ? info.offeringAssignment.shiftContext : info.requestedAssignment.shiftContext,
            otherShift: isOwnRequest ? info.requestedAssignment.shiftContext : info.offeringAssignment.shiftContext
          };
        })
      )
    ));
  }

  @Input()
  public set tradeInfo(info: TradeInfoDto | undefined) {
    this.tradeInfo$.next(info);
  }

  protected processResult(result: dialogResult, tradeData: mappedInfo) {

    if(tradeData.isOwnRequest) {
      if(result === "danger") {
        this._tradeService.cancelTrade(tradeData.tradeId).pipe(
          this._toastService.tapSuccess("Trade Cancelled", () => "The trade has been cancelled successfully."),
          this._toastService.tapError("Cancel Failed", mapValue.apiErrorToMessage)
        ).subscribe(() => this.tradeChanged.emit());
        return;
      }
    } else {
      if(result ==="success") {
        this._tradeService.acceptTrade(tradeData.tradeId).pipe(
          this._toastService.tapSuccess("Trade Accepted", () => "The trade has been accepted successfully."),
          this._toastService.tapError("Trade Failed", mapValue.apiErrorToMessage)
        ).subscribe(() => this.tradeChanged.emit());
        return;
      } else if(result === "danger") {
        this._tradeService.declineTrade(tradeData.tradeId).pipe(
          this._toastService.tapSuccess("Trade Declined", () => "The trade has been declined successfully."),
          this._toastService.tapError("Decline Failed", mapValue.apiErrorToMessage)
        ).subscribe(() => this.tradeChanged.emit());
        return;
      }
    }

    this.dialogClosed.emit();
  }

}
