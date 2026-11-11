import {Component, inject, Input, Output} from "@angular/core";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {TooltipDirective} from "../../directives/tooltip.directive";
import {icons} from "../../util/icons";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {BehaviorSubject, combineLatestWith, filter, map, Subject} from "rxjs";
import {
  AssignmentContextDto,
  EventDto, PositionSlotEndpointService, PositionSlotTradeEndpointService,
  ShiftContextDto,
  ShiftEndpointService,
  TradeInfoDto
} from "../../../shiftservice-client";
import {UserService} from "../../services/user/user.service";
import {AsyncPipe, DatePipe} from "@angular/common";
import {RouterLink} from "@angular/router";
import {ManageShiftComponent, manageShiftParams} from "../manage-shift/manage-shift.component";
import {DialogComponent} from "../dialog/dialog.component";
import {DialogTradeDetailsComponent} from "../manage-position/dialog-trade-details/dialog-trade-details.component";
import {ToastService} from "../../services/toast/toast.service";
import {mapValue} from "../../util/value-maps";

@Component({
  selector: "app-shift-trade-auction",
  imports: [
    FaIconComponent,
    TooltipDirective,
    InputButtonComponent,
    AsyncPipe,
    DatePipe,
    RouterLink,
    DialogComponent,
    ManageShiftComponent,
    DialogTradeDetailsComponent
  ],
  standalone: true,
  templateUrl: "./shift-trade-auction.component.html",
  styleUrl: "./shift-trade-auction.component.scss"
})
export class ShiftTradeAuctionComponent {

  @Output()
  public readonly itemsChanged = new Subject<void>();

  protected readonly event$ = new BehaviorSubject<EventDto | undefined>(undefined);
  protected readonly trades$ = new BehaviorSubject<TradeInfoDto[]>([]);
  protected readonly auctions$ = new BehaviorSubject<AssignmentContextDto[]>([]);

  protected readonly selectedTrade$ = new BehaviorSubject<TradeInfoDto | undefined>(undefined);
  protected readonly selectedShift$ = new BehaviorSubject<manageShiftParams | undefined>(undefined);

  protected readonly auctionData$;
  protected readonly tradeData$;

  protected readonly icons = icons;

  private readonly _userService = inject(UserService);
  private readonly _shiftService = inject(ShiftEndpointService);
  private readonly _tradeService = inject(PositionSlotTradeEndpointService);
  private readonly _positionService = inject(PositionSlotEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.tradeData$ = this.trades$.pipe(
      combineLatestWith(this._userService.userProfile$, this.event$.pipe(
        filter(event => event !== undefined)
      )),
      map(([trades, user, event]) => trades.map(trade => {
        const isOwnRequest = trade.offeringAssignment.assignment.assignedVolunteer.id === user?.account.volunteer.id;
        const otherVolunteer = isOwnRequest ?
          trade.requestedAssignment.assignment.assignedVolunteer :
          trade.offeringAssignment.assignment.assignedVolunteer;
        const otherAssignment = isOwnRequest ?
          trade.requestedAssignment :
          trade.offeringAssignment;
        const ownAssignment = isOwnRequest ?
          trade.offeringAssignment :
          trade.requestedAssignment;

        return {
          trade,
          event,
          isOwnRequest,
          otherVolunteer,
          initials: `${otherVolunteer.firstName.substring(0,1)}${otherVolunteer.lastName.substring(0,1)}`,
          otherPosition: otherAssignment.positionSlotContext,
          otherShift: trade.requestedAssignment.shiftContext,
          ownPosition: ownAssignment.positionSlotContext,
          ownShift: trade.offeringAssignment.shiftContext,
          caption: {
            pre: isOwnRequest ? "You asked " : "",
            name: isOwnRequest ?
              `${otherVolunteer.firstName} ${otherVolunteer.lastName.substring(0,1)}.` :
              `${otherVolunteer.firstName} ${otherVolunteer.lastName.substring(0,1)}.`,
            middle: isOwnRequest ? " to trade their position in " : " wants to trade their position in ",
            shiftName: otherAssignment.shiftContext.name
          }
        };
      }))
    );

    this.auctionData$ = this.auctions$.pipe(
      combineLatestWith(this._userService.userProfile$, this.event$.pipe(
        filter(event => event !== undefined)
      )),
      map(([auctions, user, event]) => auctions.map(auction => {
        const isOwnRequest = auction.assignment.assignedVolunteer.id === user?.account.volunteer.id;
        return {
          event,
          auction,
          isOwnRequest,
          initials: `${
            auction.assignment.assignedVolunteer.firstName.substring(0,1)}${auction.assignment.assignedVolunteer.lastName.substring(0,1)
          }`,
          title: `${
            auction.assignment.assignedVolunteer.firstName} ${auction.assignment.assignedVolunteer.lastName.substring(0,1)
          }. offers their position in ${auction.shiftContext.name}`,
          slot: auction.positionSlotContext,
          shift: auction.shiftContext,
          caption: {
            name: isOwnRequest ? "You" : `${
              auction.assignment.assignedVolunteer.firstName} ${auction.assignment.assignedVolunteer.lastName.substring(0,1)
            }.`,
            middle: isOwnRequest ? " offered your position in " : " offers their position in ",
            shiftName: auction.shiftContext.name
          }
        };
      }))
    );
  }

  @Input()
  public set trades(trades: TradeInfoDto[]) {
    this.trades$.next(trades.filter(trade => trade.status === TradeInfoDto.StatusEnum.Open));
  }

  @Input()
  public set auctions(auctions: AssignmentContextDto[]) {
    this.auctions$.next(auctions);
  }

  @Input()
  public set event(event: EventDto | undefined) {
    this.event$.next(event);
  }

  protected previewAuction(shift?: ShiftContextDto, event?: EventDto){
    if(shift === undefined || event === undefined) {
      throw new Error("Shift and Event are required");
    }

    this._shiftService.getShiftDetails(shift.id).subscribe(shiftDetails => this.selectedShift$.next({
      shift: shiftDetails.shift, eventId: event.id
    }));
  }

  protected previewTrade(trade: TradeInfoDto) {
    this.selectedTrade$.next(trade);
  }

  protected acceptAuction(auction: AssignmentContextDto) {
    this._positionService.claimAuction(auction.positionSlotContext.id, auction.assignment.assignedVolunteer.id, {
      acceptedRewardPointsConfigHash: auction.positionSlotContext.rewardPointsDto.rewardPointsConfigHash
    }).pipe(
      this._toastService.tapSuccess("Offer Accepted", () => "You have accepted the position offer."),
      this._toastService.tapError("Error Accepting Offer", mapValue.apiErrorToMessage)
    ).subscribe(() =>
      this.itemsChanged.next()
    );
  }

  protected cancelAuction(auction: AssignmentContextDto) {
    this._positionService.cancelAuction(auction.positionSlotContext.id).pipe(
      this._toastService.tapSuccess("Auction Canceled", () => "You have canceled your position offer."),
      this._toastService.tapError("Error Canceling Auction", mapValue.apiErrorToMessage)
    ).subscribe(() =>
      this.itemsChanged.next()
    );
  }

  protected cancelTrade(trade: TradeInfoDto) {
    this._tradeService.cancelTrade(trade.id).pipe(
      this._toastService.tapSuccess("Trade Canceled", () => "You have canceled your trade request."),
      this._toastService.tapError("Error Canceling Trade", mapValue.apiErrorToMessage)
    ).subscribe(() =>
      this.itemsChanged.next()
    );
  }

  protected acceptTrade(trade: TradeInfoDto) {
    this._tradeService.acceptTrade(trade.id).pipe(
      this._toastService.tapSuccess("Trade Accepted", () => "You have accepted the trade offer."),
      this._toastService.tapError("Error Accepting Trade", mapValue.apiErrorToMessage)
    ).subscribe(() =>
      this.itemsChanged.next()
    );
  }

  protected declineTrade(trade: TradeInfoDto) {
    this._tradeService.declineTrade(trade.id).pipe(
      this._toastService.tapSuccess("Trade Declined", () => "You have declined the trade offer."),
      this._toastService.tapError("Error Declining Trade", mapValue.apiErrorToMessage)
    ).subscribe(() =>
      this.itemsChanged.next()
    );
  }

}
