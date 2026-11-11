import {Component, inject, Input, OnDestroy, Output} from "@angular/core";
import { icons } from "../../util/icons";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {
  AssignmentDto,
  AssignmentPlannerInfoDto,
  ShiftPlanDto,
  ShiftPlanEndpointService,
  SignupEndpointService
} from "../../../shiftservice-client";
import {BehaviorSubject, combineLatestWith, filter, of, startWith, Subject, Subscription, switchMap} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {LockStatusPipe} from "../../pipes/lock-status.pipe";
import {InputMultiToggleComponent, MultiToggleOptions} from "../inputs/input-multitoggle/input-multi-toggle.component";
import LockStatusEnum = ShiftPlanDto.LockStatusEnum;
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {ToastService} from "../../services/toast/toast.service";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {RouterLink} from "@angular/router";
import {FormRouteSyncService} from "../../services/form-route-sync.service";
import {mapValue} from "../../util/value-maps";

type StatusesEnum = AssignmentDto.StatusEnum;

@Component({
  selector: "app-manage-assignments",
  imports: [
    AsyncPipe,
    LockStatusPipe,
    InputMultiToggleComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    FaIconComponent,
    RouterLink
  ],
  standalone: true,
  templateUrl: "./manage-assignments.component.html",
  styleUrl: "./manage-assignments.component.scss"
})
export class ManageAssignmentsComponent implements OnDestroy {

  @Output()
  public planChanged = new Subject<void>();

  protected readonly plan$ = new BehaviorSubject<ShiftPlanDto | undefined>(undefined);
  protected readonly assignments$ = new BehaviorSubject<AssignmentPlannerInfoDto[]>([]);
  protected readonly reloadAssignments$ = new Subject<void>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly phaseOptions: MultiToggleOptions<LockStatusEnum> = [
    { value: LockStatusEnum.SelfSignup, name: "Self Signup" },
    { value: LockStatusEnum.Supervised, name: "Supervised" },
    { value: LockStatusEnum.Locked, name: "Locked" }
  ];
  protected readonly statusOptions: MultiToggleOptions<StatusesEnum> = [
    { value: AssignmentDto.StatusEnum.Accepted, name: "Assigned" },
    { value: AssignmentDto.StatusEnum.Auction, name: "Auctioned" },
    { value: AssignmentDto.StatusEnum.AuctionRequestForUnassign, name: "Emergency Unassign" },
    { value: AssignmentDto.StatusEnum.RequestForAssignment, name: "Requested Assignment" },
  ];

  private readonly _fb = inject(FormBuilder);
  private readonly _toastService = inject(ToastService);
  private readonly _signupService = inject(SignupEndpointService);
  private readonly _planService = inject(ShiftPlanEndpointService);
  private readonly _filterSubscription: Subscription;
  private readonly _formSyncService = inject(FormRouteSyncService);

  constructor() {
    this.form = this._fb.group({
      assignmentPhase: this._fb.nonNullable.control<LockStatusEnum>(ShiftPlanDto.LockStatusEnum.SelfSignup),
      assignmentStatus: this._fb.nonNullable.control<StatusesEnum>(AssignmentDto.StatusEnum.AuctionRequestForUnassign)
    });

    this._filterSubscription = this.reloadAssignments$.pipe(
      startWith(undefined),
      switchMap(() => this.form.controls.assignmentStatus.valueChanges.pipe(
        startWith(this.form.controls.assignmentStatus.value),
        combineLatestWith(this.plan$.pipe(
          filter((plan): plan is ShiftPlanDto => plan !== undefined)
        )),
        switchMap(([status, plan]) => this._signupService.getSlots(plan.id, [status], "body"))
      ))
    ).subscribe(data => this.assignments$.next(data));

    this._formSyncService.registerForm(
      "manage-shift-plan-assignments",
      this.form,
      form => ({
        status: form.controls.assignmentStatus.value
      }),
      params => of({
        assignmentStatus: params.status !== undefined ?
          params.status as StatusesEnum :
          AssignmentDto.StatusEnum.AuctionRequestForUnassign
      })
    );
  }

  @Input()
  public set plan(plan: ShiftPlanDto | undefined) {
    this.plan$.next(plan);

    if(plan !== undefined) {
      this.form.controls.assignmentPhase.setValue(plan.lockStatus);
    } else {
      this.form.reset();
    }
  }

  ngOnDestroy() {
    this._filterSubscription.unsubscribe();
    this._formSyncService.unregisterForm("manage-shift-plan-assignments");
  }

  protected getChanges(from: LockStatusEnum, to: LockStatusEnum) {
    if(
      from === LockStatusEnum.Supervised && to === LockStatusEnum.SelfSignup ||
      from === LockStatusEnum.Locked && to === LockStatusEnum.SelfSignup
    ){
      return "During phase transition, all current assignments will be retained, but " +
        "assignments requesting for unassignment or auction will be unassigned, " +
        "and requests for assignment will be denied.\n";
    }

    return "";
  }

  protected getStatusInfo(assignmentStatus: AssignmentDto.StatusEnum) {
    switch (assignmentStatus) {
      case AssignmentDto.StatusEnum.Accepted:
        return "Volunteers that are currently assigned to a position.";
      case AssignmentDto.StatusEnum.Auction:
        return "Volunteers that want to leave a position, others can take over the position.";
      case AssignmentDto.StatusEnum.AuctionRequestForUnassign:
        return "Volunteers that will not be able to attend and requested planner attention.";
      case AssignmentDto.StatusEnum.RequestForAssignment:
        return "Volunteers that requested to be assigned to a position.";
      default:
        return "Unknown status.";
    }
  }

  protected updatePhase(plan: ShiftPlanDto) {
    const newPhase = this.form.controls.assignmentPhase.value;
    if(plan.lockStatus !== newPhase) {
      this._planService.editLockStatus(plan.id, {
        lockStatus: newPhase
      }).pipe(
        this._toastService.tapSaving("Shift Plan Phase")
      ).subscribe(() => {
        this.planChanged.next();
        this.reloadAssignments$.next();
      });
    }
  }

  protected declineRequest(shiftPlan: ShiftPlanDto, assignment: AssignmentDto) {
    this._signupService.declineRequest(shiftPlan.id, assignment.positionSlotId, assignment.assignedVolunteer.id).pipe(
      this._toastService.tapSuccess("Request Declined"),
      this._toastService.tapError("Error declining request", mapValue.apiErrorToMessage)
    ).subscribe(() => this.reloadAssignments$.next());
  }

  protected acceptRequest(shiftPlan: ShiftPlanDto, assignment: AssignmentDto) {
    this._signupService.acceptRequest(shiftPlan.id, assignment.positionSlotId, assignment.assignedVolunteer.id).pipe(
      this._toastService.tapSuccess("Request Approved"),
      this._toastService.tapError("Error approving request", mapValue.apiErrorToMessage)
    ).subscribe(() => this.reloadAssignments$.next());
  }

  protected unassign(shiftPlan: ShiftPlanDto, assignment: AssignmentDto) {
    this._signupService.unAssignUsersFromSlot(shiftPlan.id, {
      volunteerIds: [assignment.assignedVolunteer.id],
      positionSlotId: assignment.positionSlotId
    }).pipe(
      this._toastService.tapSuccess("User Unassigned"),
      this._toastService.tapError("Error unassigning user", mapValue.apiErrorToMessage)
    ).subscribe(() => this.reloadAssignments$.next());
  }

}
