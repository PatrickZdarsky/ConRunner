import {Component, EventEmitter, inject, Input, OnDestroy, Output} from "@angular/core";
import {InputSelectComponent, SelectOptions} from "../inputs/input-select/input-select.component";
import {BehaviorSubject, catchError, combineLatestWith, map, Observable, of, startWith, Subscription, switchMap} from "rxjs";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {UserService} from "../../services/user/user.service";
import {AsyncPipe, DatePipe, NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {DialogComponent} from "../dialog/dialog.component";
import {TooltipDirective} from "../../directives/tooltip.directive";
import {InputNumberComponent} from "../inputs/input-number/input-number.component";
import {InputToggleComponent} from "../inputs/input-toggle/input-toggle.component";
import {icons} from "../../util/icons";
import {PositionSignupComponent, positionSignupParams} from "./position-signup/position-signup.component";
import PositionSignupStateEnum = PositionSlotDto.PositionSignupStateEnum;
import {ToastService} from "../../services/toast/toast.service";
import {descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";
import {mapValue} from "../../util/value-maps";
import {PositionSlotDto, PositionSlotEndpointService, RoleDto, ShiftDto, VolunteerDto} from "../../../shiftservice-client";

export interface managePositionParams {
  shift: ShiftDto;
  position?: PositionSlotDto;
  availableRoles: SelectOptions<RoleDto>;
}

@Component({
  selector: "app-manage-position",
  imports: [
    AsyncPipe,
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputSelectComponent,
    InputButtonComponent,
    DialogComponent,
    TooltipDirective,
    InputNumberComponent,
    InputToggleComponent,
    NgClass,
    PositionSignupComponent
  ],
  providers: [
    DatePipe
  ],
  templateUrl: "./manage-position.component.html",
  styleUrl: "./manage-position.component.scss"
})
export class ManagePositionComponent implements OnDestroy {

  @Output()
  public readonly positionChanged = new EventEmitter<PositionSlotDto>();

  public readonly form;

  protected readonly icons = icons;

  protected readonly manageData$ = new BehaviorSubject<undefined | managePositionParams>(undefined);
  protected readonly positionSignupData$: Observable<positionSignupParams | undefined>;
  protected readonly requestedEditMode$ = new BehaviorSubject<boolean>(false);

  protected showSlotDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _userService = inject(UserService);
  private readonly _positionService = inject(PositionSlotEndpointService);
  private readonly _toastService = inject(ToastService);

  private readonly _updatePointsDisplaySubscription: Subscription;

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [nameLengthValidator, Validators.required]),
      description: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      desiredVolunteerCount: this._fb.nonNullable.control<number>(5, [Validators.min(1), Validators.required]),
      skipAutoAssignment: this._fb.nonNullable.control<boolean>(false),
      role: this._fb.control<RoleDto | null>(null),
      enableCustomPoints: this._fb.nonNullable.control<boolean>(false),
      rewardPoints: this._fb.nonNullable.control<number>(0, [Validators.min(0)])
    });

    this._updatePointsDisplaySubscription = this.form.valueChanges.pipe(
      startWith(this.form.value),
    ).subscribe(value => {
      const customEnabled = value.enableCustomPoints ?? false;
      const pointsEnabled = this.form.controls.rewardPoints.enabled;

      if(customEnabled && !pointsEnabled) {
        this.form.controls.rewardPoints.enable();
      }
      if(!customEnabled && pointsEnabled) {
        this.form.controls.rewardPoints.disable();
      }
    });

    this.positionSignupData$ = this.manageData$.pipe(
      switchMap((data) => {
        if(data === undefined || data.position === undefined) {
          return of(undefined);
        }
        const position = data.position;

        /* necessary because signup requests are not shown in public position data */
        return this._positionService.getPositionSlotUserAssignment(data.position.id).pipe(
          catchError(() => of(undefined)),
          map(currentUserAssignment => ({
            slot: position,
            shift: data.shift,
            currentUserAssignment
          })),
        );
      })
    );
  }

  public get mode$(){
    return this.manageData$.pipe(
      combineLatestWith(this.canManage$, this.requestedEditMode$),
      map(([data, canManage, requestedEdit]) => {
        if(data?.position === undefined && canManage) {
          return "create";
        }
        if(requestedEdit && canManage && data !== undefined) {
          return "edit";
        }
        return "view";
      })
    );
  }

  public get canManage$() {
    return this.manageData$.pipe(
      switchMap(data => data === undefined ?
        of(false) :
        this._userService.canManagePlan$(data.shift.shiftPlan.id)
      )
    );
  }

  @Input()
  public set manageData(value: managePositionParams) {
    this.manageData$.next(value);

    if (value.position) {
      this.form.setValue({
        name: value.position.name,
        description: value.position.description ?? "",
        desiredVolunteerCount: value.position.desiredVolunteerCount,
        skipAutoAssignment: value.position.skipAutoAssignment,
        role: value.position.role ?? null,
        rewardPoints: value.position.rewardPointsDto.overrideRewardPoints ?? 0,
        enableCustomPoints: value.position.rewardPointsDto.overrideRewardPoints !== undefined
      });
    } else {
      this.form.setValue({
        name: "",
        description: "",
        desiredVolunteerCount: 5,
        skipAutoAssignment: false,
        role: null,
        rewardPoints: 0,
        enableCustomPoints: false
      });
    }
  }

  public ngOnDestroy(): void {
    this._updatePointsDisplaySubscription?.unsubscribe();
  }

  protected create(shiftId: string) {
    this.form.markAllAsTouched();

    if(this.form.valid) {

      this._positionService.createPositionSlotInShift(shiftId, {
        name: this.form.controls.name.value,
        description: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
        desiredVolunteerCount: this.form.controls.desiredVolunteerCount.value,
        skipAutoAssignment: this.form.controls.skipAutoAssignment.value,
        roleId: this.form.controls.role.value?.id ?? undefined,
        overrideRewardPoints: this.form.value.rewardPoints /* automatically undefined if disabled */
      }).pipe(
        this._toastService.tapCreating("Position", item => item.name)
      ).subscribe(pos => {
        this.positionChanged.emit(pos);
      });
    } else {
      this._toastService.showError("Invalid Position", "Please provide valid position details.");
    }
  }

  protected update(position: PositionSlotDto) {
    this.form.markAllAsTouched();

    if(this.form.valid) {
      this._positionService.updatePositionSlot(position.id, {
        name: this.form.controls.name.value,
        description: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
        desiredVolunteerCount: this.form.controls.desiredVolunteerCount.value,
        skipAutoAssignment: this.form.controls.skipAutoAssignment.value,
        roleId: this.form.controls.role.value?.id ?? undefined,
        overrideRewardPoints: this.form.value.rewardPoints /* automatically undefined if disabled */
      }).pipe(
        this._toastService.tapSaving("Position", item => item.name)
      ).subscribe(pos => {
        this.positionChanged.emit(pos);
      });
    } else {
      this._toastService.showError("Invalid Position", "Please provide valid position details.");
    }
  }

  protected delete(position: PositionSlotDto) {
    this._positionService.deletePositionSlot(position.id).pipe(
      this._toastService.tapDeleting("Position", () => position.name)
    ).subscribe(() => {
      this.positionChanged.emit();
    });
  }

  protected idComparatorFn(a: { id: string } | null, b: { id: string } | null): boolean {
    return a?.id === b?.id || (a === null && b === null);
  }

  protected hasTimeConflict(position?: PositionSlotDto): boolean {
    if(position === undefined) {
      return false;
    }

    return position.positionSignupState !== PositionSignupStateEnum.NotEligible &&
      (position.positionSignupState === PositionSignupStateEnum.TimeConflictAssignment ||
      position.positionSignupState === PositionSignupStateEnum.TimeConflictTimeConstraint);
  }

  protected isEligible(position?: PositionSlotDto): boolean {
    if(position === undefined) {
      return false;
    }

    return position.positionSignupState !== PositionSignupStateEnum.NotEligible;
  }

  protected isSignedUp(position?: PositionSlotDto): boolean {
    if(position === undefined) {
      return false;
    }

    return position.positionSignupState === PositionSignupStateEnum.SignedUp;
  }

  protected getOrder(position?: PositionSlotDto) {
    const order = Number(position?.id) * -1;
    if(isNaN(order)) {
      return Number.MIN_SAFE_INTEGER;
    }
    return order;
  }

  protected getVolunteerUsername(volunteer: VolunteerDto) {
    return volunteer.username ?? "";
  }

  protected getVolunteerInitials(username?: string) {
    return username?.charAt(0)?.toUpperCase() || "";
  }
}
