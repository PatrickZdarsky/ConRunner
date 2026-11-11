import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {BehaviorSubject, map, of, switchMap} from "rxjs";
import {
  AssignmentDto,
  PositionSlotDto,
  PositionSlotEndpointService, PositionSlotTradeEndpointService,
  ShiftDto, ShiftPlanDto, TradeCandidatesDto, TradeInfoDto, VolunteerDto, TradeCreateDto
} from "../../../../shiftservice-client";
import {icons} from "../../../util/icons";
import {AsyncPipe, SlicePipe} from "@angular/common";
import {InputButtonComponent} from "../../inputs/input-button/input-button.component";
import {ToastService} from "../../../services/toast/toast.service";
import {mapValue} from "../../../util/value-maps";
import LockStatusEnum = ShiftPlanDto.LockStatusEnum;
import {DialogTradeRequestComponent} from "../dialog-trade-request/dialog-trade-request.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {DialogTradeDetailsComponent} from "../dialog-trade-details/dialog-trade-details.component";
import {UserService} from "../../../services/user/user.service";
import {DialogManageAssignmentsComponent, manageAssignmentsParams} from "../dialog-manage-assignments/dialog-manage-assignments.component";

export interface positionSignupParams {
  slot: PositionSlotDto;
  shift: ShiftDto;
  currentUserAssignment?: AssignmentDto;
}

interface signupOptions {
  availableTrades: TradeInfoDto[];
  slot: PositionSlotDto;
}

interface tradeRequestOptions {
  slot: PositionSlotDto;
  candidates: TradeCandidatesDto[];
}

@Component({
  selector: "app-position-signup",
  imports: [
    AsyncPipe,
    InputButtonComponent,
    DialogTradeRequestComponent,
    SlicePipe,
    FaIconComponent,
    DialogTradeDetailsComponent,
    DialogManageAssignmentsComponent
  ],
  standalone: true,
  templateUrl: "./position-signup.component.html",
  styleUrl: "./position-signup.component.scss"
})
export class PositionSignupComponent {

  @Output()
  public positionSignupChanged = new EventEmitter<AssignmentDto | undefined>();

  protected readonly icons = icons;

  protected readonly signupOptions$ = new BehaviorSubject<signupOptions | undefined>(undefined);
  protected readonly tradeRequestOptions$ = new BehaviorSubject<tradeRequestOptions | undefined>(undefined);
  protected readonly dialogTradeInfo$ = new BehaviorSubject<TradeInfoDto | undefined>(undefined);
  protected readonly dialogAssignmentsParams$ = new BehaviorSubject<manageAssignmentsParams | undefined>(undefined);
  protected readonly position$ = new BehaviorSubject<positionSignupParams | undefined>(undefined);
  protected readonly canParticipate$ = this.position$.pipe(
    switchMap(data => data === undefined ?
      of(false) :
      this.userService.canParticipatePlan$(data.shift.shiftPlan.id)
    )
  );
  protected readonly header$ = this.position$.pipe(
    map(position => this.getHeader(position?.slot))
  );
  protected readonly body$ = this.position$.pipe(
    map(position => this.getBody(position))
  );
  protected readonly actions$ = this.position$.pipe(
    map(position => this.getActions(position))
  );

  protected readonly userService = inject(UserService);

  private readonly _positionService = inject(PositionSlotEndpointService);
  private readonly _tradeService = inject(PositionSlotTradeEndpointService);
  private readonly _toastService = inject(ToastService);

  public get canManage$() {
    return this.position$.pipe(
      switchMap(data => data === undefined ?
        of(false) :
        this.userService.canManagePlan$(data.shift.shiftPlan.id)
      )
    );
  }

  public get user$() {
    return this.userService.userProfile$;
  }

  @Input()
  public set positionSlot(value: positionSignupParams | undefined) {
    this.position$.next(value);
  }

  /**
   * Open manage assignments dialog for planners
   * @param slot
   * @protected
   */
  protected manageAssignments(slot: PositionSlotDto) {
    this.dialogAssignmentsParams$.next({
      position: slot
    });
  }

  /**
   * Initiate signup with direct assignment
   * @param slot
   * @param shift
   * @param userId
   * @protected
   */
  protected signUp(slot: PositionSlotDto, shift: ShiftDto) {

    /* during self-signup phase, users can freely sign up if free slots */
    if(
      shift.lockStatus === LockStatusEnum.SelfSignup &&
      (
        slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupPossible ||
        slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupOrTrade
      )
    ) {
      this._positionService.joinPositionSlot(slot.id, {
        acceptedRewardPointsConfigHash: slot.rewardPointsDto.rewardPointsConfigHash
      }).pipe(
        this._toastService.tapSuccess("Signed Up",
          () => `You are now signed up for the position "${slot.name}". You can view your assignments in the shift plan dashboard.`),
        this._toastService.tapError("Could Not Sign Up", mapValue.apiErrorToMessage)
      ).subscribe(data => {
        this.positionSignupChanged.emit(data);
      });
      return;
    }

    /* during supervised phase, users can accept auctions */
    if(
      shift.lockStatus === LockStatusEnum.Supervised &&
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaAuction
    ) {
      const auctionToAccept = slot.auctions[0];
      this._positionService.claimAuction(auctionToAccept.positionSlotId, auctionToAccept.assignedVolunteer.id, {
        acceptedRewardPointsConfigHash: slot.rewardPointsDto.rewardPointsConfigHash
      }).pipe(
        this._toastService.tapSuccess("Signed Up",
          () => `You are now signed up for the position "${slot.name}". You can view your assignments in the shift plan dashboard.`),
        this._toastService.tapError("Could Not Sign Up", mapValue.apiErrorToMessage)
      ).subscribe(data => {
        this.positionSignupChanged.emit(data);
      });
      return;
    }

    /* during both phases, users can directly sign up if trades are available */
    if(
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupOrTrade ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaTrade
    ) {
      if(slot.offeredTrades.length === 0) {
        throw new Error("No available trades for signing up to position.");
      }

      /* handle selection in modal */
      this.signupOptions$.next({
        availableTrades: slot.offeredTrades,
        slot: slot
      });
      return;
    }

    throw new Error("No available options for signing up to position.");
  }

  /**
   * Initiate signup that requires approval from another party
   * @param slot
   * @param shift
   * @protected
   */
  protected requestSignUp(slot: PositionSlotDto, shift: ShiftDto) {

    /* during supervised, if a regular signup would be possible, allow a signup request to be accepted by the admin */
    if(shift.lockStatus === LockStatusEnum.Supervised &&
      (slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupPossible ||
        slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupOrTrade)
    ) {
      this._positionService.joinRequestPositionSlot(slot.id).pipe(
        this._toastService.tapSuccess("Requested Signup",
          () => `You requested to sign up to the position "${slot.name}". A shift planner will handle your request.`),
        this._toastService.tapError("Could Not Request Signup", mapValue.apiErrorToMessage)
      ).subscribe(data => {
        this.positionSignupChanged.emit(data);
      });
      return;
    }

    throw new Error("No available options for requesting position sign-up.");
  }

  /**
   * Initiate sign-out that requires approval from another party
   * @param slot
   * @param shift
   * @param assignment
   * @protected
   */
  protected requestSignOut(slot: PositionSlotDto, shift: ShiftDto, assignment: AssignmentDto | undefined) {

    /* during supervised phase, users can request to be unassigned */
    if(
      shift.lockStatus === LockStatusEnum.Supervised &&
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignedUp &&
      assignment !== undefined && (
        assignment.status === AssignmentDto.StatusEnum.Accepted ||
        assignment.status === AssignmentDto.StatusEnum.Auction
      )
    ) {
      if(assignment.status === AssignmentDto.StatusEnum.Auction) {
        this._positionService.leaveRequestPositionSlot(slot.id).pipe(
          this._toastService.tapSuccess("Requested Planner Attention",
            () => `You requested planner attention for your request in "${slot.name}". A shift planner will handle your request.`),
          this._toastService.tapError("Could Not Request Unassignment", mapValue.apiErrorToMessage)
        ).subscribe(data => {
          this.positionSignupChanged.emit(data);
        });
      } else if (assignment.status === AssignmentDto.StatusEnum.Accepted) {
        this._positionService.auctionAssignment(slot.id).pipe(
          this._toastService.tapSuccess("Requested Unassignment",
            () => `You requested to be unassigned from the position "${slot.name}"
            . Until another volunteer takes your spot, you are still signed up.`),
          this._toastService.tapError("Could Not Request Unassignment", mapValue.apiErrorToMessage)
        ).subscribe(data => {
          this.positionSignupChanged.emit(data);
        });
      }
    }

    throw new Error("No available options for requesting position sign-out.");
  }

  /**
   * Initiate trade request user selection UI
   * @param slot
   * @param shift
   * @protected
   */
  protected requestTrade(slot: PositionSlotDto) {

    /* if eligible and not in position, users can start a trade */
    if(
      slot.positionSignupState !== PositionSlotDto.PositionSignupStateEnum.SignedUp &&
      slot.positionSignupState !== PositionSlotDto.PositionSignupStateEnum.NotEligible
    ) {
      this._tradeService.getPositionSlotsToOffer(slot.id).subscribe(candidates => {
        this.tradeRequestOptions$.next({
          slot: slot,
          candidates: candidates
        });
      });
      return;
    }

    throw new Error("No available options for requesting trade.");
  }

  /**
   * Submit trade request with given selection
   * @param slot
   * @param volunteer
   * @param userId
   * @param offeringSlot
   * @protected
   */
  protected submitTradeRequest(slot: PositionSlotDto, volunteer: VolunteerDto | undefined, offeringSlot: PositionSlotDto) {
    const requestedAssignment = slot.assignments.find(ass => ass.assignedVolunteer.id === volunteer?.id);

    if(requestedAssignment === undefined) {
      throw new Error("requested trading assignment couldnt not be determined");
    }

    const trade: TradeCreateDto = {
      requestedPositionSlotId: requestedAssignment.positionSlotId,
      offeredPositionSlotId: offeringSlot.id,
      requestedVolunteerIds: [requestedAssignment.assignedVolunteer.id] // TODO make multiselect
    };

    this._tradeService.createTrade(trade).pipe(
      this._toastService.tapSuccess("Requested Trade",
        () => "The other volunteer will be notified. You can see the status on your shift plan dashboard."),
      this._toastService.tapError("Could Request Trade", mapValue.apiErrorToMessage)
    ).subscribe(() => this.positionSignupChanged.emit(undefined));
  }

  protected signOut(slot: PositionSlotDto) {
    this._positionService.leavePositionSlot(slot.id).pipe(
      this._toastService.tapSuccess("Successfully Unassigned",
        () => `You are no longer signed up for the position ${slot.name}.`),
      this._toastService.tapError("Could Not Unassign", mapValue.apiErrorToMessage)
    ).subscribe(data => {
      this.positionSignupChanged.emit(data);
    });
  }

  protected cancelSignOutRequest(slot: PositionSlotDto, shift: ShiftDto, assignment: AssignmentDto | undefined) {

    /* during supervised phase, while being signed up, users can cancel the request to be unassigned/cancel the auction */
    if(
      shift.lockStatus === LockStatusEnum.Supervised &&
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignedUp &&
      assignment !== undefined &&
      (assignment.status === AssignmentDto.StatusEnum.AuctionRequestForUnassign || assignment.status === AssignmentDto.StatusEnum.Auction)
    ) {
      if(assignment.status === AssignmentDto.StatusEnum.Auction) {
        this._positionService.cancelAuction(slot.id).pipe(
          this._toastService.tapSuccess("Cancelled Leave Request",
            () => `You have cancelled your leave request for the position "${slot.name}". You are still signed up.`),
          this._toastService.tapError("Could Not Cancel leave Request", mapValue.apiErrorToMessage)
        ).subscribe(data => {
          this.positionSignupChanged.emit(data);
        });
      } else if (assignment.status === AssignmentDto.StatusEnum.AuctionRequestForUnassign) {
        this._positionService.leaveRequestWithdrawPositionSlot(slot.id).pipe(
          this._toastService.tapSuccess("Cancelled Leave Request",
            () => `You have cancelled your leave request for the position "${slot.name}". You are still signed up.`),
          this._toastService.tapError("Could Not Cancel Leave Request", mapValue.apiErrorToMessage)
        ).subscribe(data => {
          this.positionSignupChanged.emit(data);
        });
      }
    }

    throw new Error("No available options for cancel requested position sign-out.");
  }

  protected cancelSignUpRequest(slot: PositionSlotDto, shift: ShiftDto, assignment: AssignmentDto | undefined) {

    /* during supervised phase, while not being signed up, users can cancel the request to be assigned */
    if(
      shift.lockStatus === LockStatusEnum.Supervised &&
      slot.positionSignupState !== PositionSlotDto.PositionSignupStateEnum.SignedUp &&
      assignment !== undefined && assignment.status === AssignmentDto.StatusEnum.RequestForAssignment
    ) {
      this._positionService.joinRequestWithdrawPositionSlot(slot.id).pipe(
        this._toastService.tapSuccess("Cancelled Sign Up Request",
          () => `You have cancelled your sign up request for the position "${slot.name}". You are not signed up.`),
        this._toastService.tapError("Could Not Cancel Sign Up Request", mapValue.apiErrorToMessage)
      ).subscribe(data => {
        this.positionSignupChanged.emit(data);
      });
    }

    throw new Error("No available options for cancel requested position sign-up.");
  }

  private getHeader(position: PositionSlotDto | undefined): string | undefined {
    if(position?.positionSignupState !== PositionSlotDto.PositionSignupStateEnum.SignedUp) {
      return undefined;
    }

    return "You are signed up for this position!";
  }

  private getBody(position: positionSignupParams | undefined): string | undefined {
    if(position === undefined) {
      return undefined;
    }

    /* special case: assignment with request for (un)assign */
    if(position.currentUserAssignment !== undefined) {
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.RequestForAssignment) {
        return "You have requested to be signed up.\nYou will be notified when staff accepts or denies the request.";
      }
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.Auction) {
        return "You have requested to be unassigned and your position is up for auction.\n" +
          "You will be notified when another volunteer takes your position.\n" +
          "Until then, you are still signed up!";
      }
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.AuctionRequestForUnassign) {
        return "You have requested to be unassigned urgently and notified staff.\n" +
          "You will be notified when staff accepts or denies the request, or another volunteer takes your position.\n" +
          "Until then, you are still signed up!";
      }
    }

    switch(position.slot.lockStatus) {

      case ShiftDto.LockStatusEnum.SelfSignup: {

        switch(position.slot.positionSignupState) {
          case PositionSlotDto.PositionSignupStateEnum.SignupViaAuction:
            return "You are eligible to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.SignupPossible:
            return "You are eligible to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.SignupViaTrade:
            return "You are eligible to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.SignupOrTrade:
            return "You are eligible to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.SignedUp:
            return "Make sure to show up at the shift location at the specified time.";
          case PositionSlotDto.PositionSignupStateEnum.Full:
            return "The position slot is currently full.";
          case PositionSlotDto.PositionSignupStateEnum.NotEligible:
            return "You don't have the required roles to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictAssignment:
            return "You have signed up for another shift at the same time.";
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictTimeConstraint:
            return "The shift is during one of your unavailable times.";
          default:
            return "Position eligibility could not be determined.";
        }
      }

      case ShiftDto.LockStatusEnum.Supervised: {

        switch(position.slot.positionSignupState) {
          case PositionSlotDto.PositionSignupStateEnum.SignupPossible:
            return "Positions are currently locked.\n You can request a sign-up which will be checked by staff.";
          case PositionSlotDto.PositionSignupStateEnum.SignupViaTrade:
            return "Positions are currently locked, but you can accept a trade.";
          case PositionSlotDto.PositionSignupStateEnum.SignupViaAuction:
            return "Positions are currently locked, but you can accept an auction.";
          case PositionSlotDto.PositionSignupStateEnum.SignupOrTrade:
            return "Positions are currently locked.\n" +
              "You can request a sign-up which will be checked by staff, or ask for a trade with another volunteer.";
          case PositionSlotDto.PositionSignupStateEnum.SignedUp:
            return `Make sure to show up at the location ${position.shift.location?.name} at the specified time.`;
          case PositionSlotDto.PositionSignupStateEnum.NotEligible:
            return "You don't have the required roles to sign up for this position.";
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictAssignment:
            return "You have signed up for another shift at the same time.";
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictTimeConstraint:
            return "The shift is during one of your unavailable times.";
          case PositionSlotDto.PositionSignupStateEnum.Full:
            return "The position is already fully staffed.";
          default:
            return "Position eligibility could not be determined.";
        }
      }

      default: {
        return "Positions are currently locked.";
      }
    }
  }

  private getActions(position: positionSignupParams | undefined): Array<
    "SIGN_UP" | "REQUEST_TRADE" | "SIGN_OUT" | "REQUEST_SIGN_UP" | "DISABLED_SIGN_UP" |
    "CANCEL_REQUEST_SIGN_UP" | "CANCEL_REQUEST_SIGN_OUT" | "REQUEST_SIGN_OUT" | "REQUEST_UNASSIGN_ATTENTION"> | undefined {
    if (position === undefined) {
      return undefined;
    }

    const REQUEST_TRADE = position.slot.assignments.length > 0 ? ["REQUEST_TRADE"] as const : [] as const;

    /* special case: assignment with request for (un)assign */
    if(position.currentUserAssignment !== undefined) {
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.RequestForAssignment) {
        return ["CANCEL_REQUEST_SIGN_UP", ...REQUEST_TRADE]; // can create trade to this position while being in requested state
      }
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.AuctionRequestForUnassign) {
        return ["CANCEL_REQUEST_SIGN_OUT"]; // is still signed up, cant trade to this position
      }
      if(position.currentUserAssignment.status === AssignmentDto.StatusEnum.Auction) {
        return ["CANCEL_REQUEST_SIGN_OUT", "REQUEST_UNASSIGN_ATTENTION"]; // is still signed up, cant trade to this position, can elevate
      }
    }

    switch(position.slot.lockStatus) {

      case ShiftDto.LockStatusEnum.SelfSignup: {

        switch(position.slot.positionSignupState) {
          case PositionSlotDto.PositionSignupStateEnum.SignupPossible:
          case PositionSlotDto.PositionSignupStateEnum.SignupViaAuction:
            return ["SIGN_UP"];
          case PositionSlotDto.PositionSignupStateEnum.SignupOrTrade:
          case PositionSlotDto.PositionSignupStateEnum.SignupViaTrade:
            return ["SIGN_UP", ...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.SignedUp:
            return ["SIGN_OUT"];
          case PositionSlotDto.PositionSignupStateEnum.Full:
            return [...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.NotEligible:
            return ["DISABLED_SIGN_UP"];
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictAssignment:
            return [...REQUEST_TRADE];
          default:
            return undefined;
        }
      }

      case ShiftDto.LockStatusEnum.Supervised: {

        switch(position.slot.positionSignupState) {
          case PositionSlotDto.PositionSignupStateEnum.SignupPossible:
            return ["REQUEST_SIGN_UP", ...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.SignupViaAuction:
          case PositionSlotDto.PositionSignupStateEnum.SignupViaTrade:
            return ["SIGN_UP", ...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.SignupOrTrade:
            return ["REQUEST_SIGN_UP", ...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.SignedUp:
            return ["REQUEST_SIGN_OUT"];
          case PositionSlotDto.PositionSignupStateEnum.Full:
            return [...REQUEST_TRADE];
          case PositionSlotDto.PositionSignupStateEnum.NotEligible:
            return ["DISABLED_SIGN_UP"];
          case PositionSlotDto.PositionSignupStateEnum.TimeConflictAssignment:
            return [...REQUEST_TRADE];
          default:
            return undefined;
        }
      }

      default: {
        return undefined;
      }
    }
  }
}
