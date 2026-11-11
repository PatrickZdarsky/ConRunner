import {Component, inject, Input, Output} from "@angular/core";
import {
  RoleDto, RoleEndpointService, RoleModificationDto, ShiftPlanDto
} from "../../../shiftservice-client";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {mapValue} from "../../util/value-maps";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {AsyncPipe, NgClass} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {icons} from "../../util/icons";
import {InputNumberComponent} from "../inputs/input-number/input-number.component";
import {ToastService} from "../../services/toast/toast.service";
import {BehaviorSubject, Subject} from "rxjs";
import {descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";

@Component({
  selector: "app-manage-role",
  imports: [
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    NgClass,
    DialogComponent,
    InputNumberComponent,
    AsyncPipe
  ],
  standalone: true,
  templateUrl: "./manage-role.component.html",
  styleUrl: "./manage-role.component.scss"
})
export class ManageRoleComponent {

  @Output()
  roleChanged = new Subject<void>();

  protected readonly manageData$ =
    new BehaviorSubject<undefined | { plan: ShiftPlanDto; role: RoleDto | undefined }>(undefined);

  protected readonly icons = icons;
  protected readonly form;

  protected showDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _roleService = inject(RoleEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [Validators.required, nameLengthValidator]),
      description: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      rewardPointsPerMinute: this._fb.nonNullable.control<number>(0, [Validators.min(0)])
    });
  }

  @Input()
  set manageData(data: { plan: ShiftPlanDto; role: RoleDto | undefined } | undefined) {
    this.manageData$.next(data);

    if(data?.role !== undefined) {
      this.form.controls.name.setValue(data.role.name);
      this.form.controls.description.setValue(data.role.description ?? "");
      this.form.controls.rewardPointsPerMinute.setValue(data.role.rewardPointsPerMinute);
    } else {
      this.form.reset();
    }
  }

  protected save(role: RoleDto | undefined, plan: ShiftPlanDto) {
    this.form.markAllAsTouched();

    if(this.form.invalid) {
      this._toastService.showError("Invalid Role", "Please provide valid role details.");
      return;
    }

    const roleData: RoleModificationDto = {
      name: this.form.controls.name.value,
      description: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
      selfAssignable: false,
      rewardPointsPerMinute: this.form.controls.rewardPointsPerMinute.value
    };

    (role === undefined ?
      this._roleService.createRole(plan.id, roleData) :
      this._roleService.updateRole(role.id, roleData)
    ).pipe(
      role === undefined ?
        this._toastService.tapCreating("Role", item => item.name) :
        this._toastService.tapSaving("Role", item => item.name)
    ).subscribe(() => {
      this.roleChanged.next();

      if(role === undefined) {
        this.form.reset();
      }
    });
  }

  protected delete(role: RoleDto) {
    this._roleService.deleteRole(role.id).pipe(
      this._toastService.tapDeleting("Role")
    ).subscribe(() =>{
      this.roleChanged.next();
    });
  }

  protected getOrder(role?: RoleDto) {
    const order = Number(role?.id) * -1;
    if(isNaN(order)) {
      return Number.MIN_SAFE_INTEGER;
    }
    return order;
  }
}
