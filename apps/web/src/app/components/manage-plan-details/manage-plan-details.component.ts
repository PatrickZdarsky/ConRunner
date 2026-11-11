import {Component, inject, Input, Output} from "@angular/core";
import {BehaviorSubject, Subject} from "rxjs";
import {
  ShiftPlanDto,
  ShiftPlanEndpointService
} from "../../../shiftservice-client";
import { icons } from "../../util/icons";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {ToastService} from "../../services/toast/toast.service";
import {AsyncPipe, NgClass} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {InputNumberComponent} from "../inputs/input-number/input-number.component";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {captionLengthValidator, descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";
import {mapValue} from "../../util/value-maps";

@Component({
  selector: "app-manage-plan-details",
  imports: [
    AsyncPipe,
    DialogComponent,
    FaIconComponent,
    InputButtonComponent,
    InputNumberComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    NgClass
  ],
  templateUrl: "./manage-plan-details.component.html",
  styleUrl: "./manage-plan-details.component.scss"
})
export class ManagePlanDetailsComponent {

  @Output()
  planChanged = new Subject<void>();

  protected readonly form;
  protected readonly icons = icons;

  protected readonly manageData$ =
    new BehaviorSubject<undefined | { plan: ShiftPlanDto | undefined; eventId: string}>(undefined);

  protected showPlanDeleteConfirm = false;
  private readonly _fb = inject(FormBuilder);
  private readonly _planService = inject(ShiftPlanEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [nameLengthValidator, Validators.required]),
      shortDescription: this._fb.nonNullable.control<string>("", [captionLengthValidator]),
      longDescription: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      defaultRewardPointsPerMinute: this._fb.nonNullable.control<number>(10, [Validators.min(0)])
    });
  }

  @Input()
  public set manageData(data: { plan: ShiftPlanDto | undefined; eventId: string} | undefined) {
    this.manageData$.next(data);

    if(data?.plan !== undefined) {
      this.form.setValue({
        name: data.plan.name,
        shortDescription: data.plan.shortDescription ?? "",
        longDescription: data.plan.longDescription ?? "",
        defaultRewardPointsPerMinute: data.plan.defaultNoRolePointsPerMinute
      });
    } else {
      this.form.reset();
    }
  }

  protected create(eventId: string) {

    this.form.markAllAsTouched();

    if(this.form.valid) {
      this._planService.createShiftPlan(eventId, {
        name: this.form.controls.name.value,
        shortDescription: mapValue.undefinedIfEmptyString(this.form.controls.shortDescription.value),
        longDescription: mapValue.undefinedIfEmptyString(this.form.controls.longDescription.value),
        defaultNoRolePointsPerMinute: this.form.controls.defaultRewardPointsPerMinute.value
      }).pipe(
        this._toastService.tapCreating("Shift Plan", item => item.shiftPlan.name)
      ).subscribe(() => this.planChanged.next());
    } else {
      this._toastService.showError("Invalid Shift Plan", "Please provide valid shift plan details.");
    }
  }

  protected delete(shiftPlan: ShiftPlanDto) {

    this._planService.deleteShiftPlan(shiftPlan.id).pipe(
      this._toastService.tapDeleting("Shift Plan")
    ).subscribe(() => this.planChanged.next());
  }

  protected update(shiftPlan: ShiftPlanDto) {

    this.form.markAllAsTouched();

    if(this.form.valid) {
      this._planService.updateShiftPlan(shiftPlan.id, {
        name: this.form.controls.name.value,
        shortDescription: this.form.controls.shortDescription.value,
        longDescription: this.form.controls.longDescription.value,
        defaultNoRolePointsPerMinute: this.form.controls.defaultRewardPointsPerMinute.value
      }).pipe(
        this._toastService.tapSaving("Shift Plan", item => item.name)
      ).subscribe(() => this.planChanged.next());
    } else {
      this._toastService.showError("Invalid Shift Plan", "Please provide valid shift plan details.");
    }
  }

  protected getOrder(plan?: ShiftPlanDto) {
    const order = -1 * Number(plan?.id);
    if(isNaN(order)) {
      return Number.MIN_SAFE_INTEGER;
    }
    return order;
  }
}
