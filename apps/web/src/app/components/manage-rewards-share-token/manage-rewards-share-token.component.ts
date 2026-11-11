import {Component, inject, Input, Output} from "@angular/core";
import {
  RewardPointsEndpointService, RewardPointsShareTokenCreateRequestDto, RewardPointsShareTokenDto
} from "../../../shiftservice-client";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {AsyncPipe, DatePipe, NgClass} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {BehaviorSubject, Subject} from "rxjs";
import {nameLengthValidator} from "../../util/textValidators";
import {environment} from "../../environment";

@Component({
  selector: "app-manage-rewards-share-token",
  imports: [
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    NgClass,
    DialogComponent,
    AsyncPipe,
    DatePipe
  ],
  templateUrl: "./manage-rewards-share-token.component.html",
  styleUrl: "./manage-rewards-share-token.component.scss"
})
export class ManageRewardsShareTokenComponent {

  @Output()
  public tokenChanged = new Subject<void>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly manageData$ =
    new BehaviorSubject<undefined | { token: RewardPointsShareTokenDto | undefined }>(undefined);

  protected showDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _rewardsService = inject(RewardPointsEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [Validators.required, nameLengthValidator])
    });
  }

  @Input()
  public set manageData(data: { token: RewardPointsShareTokenDto | undefined } | undefined) {
    this.manageData$.next(data);

    this.form.reset();

    if(data !== undefined && data.token !== undefined) {
      this.form.controls.name.setValue(data.token.name);
    }
  }

  protected create() {
    this.form.markAllAsTouched();

    if(this.form.invalid) {
      this._toastService.showError("Invalid Share", "Please provide valid share details.");
      return;
    }

    const tokenData: RewardPointsShareTokenCreateRequestDto = {
      name: this.form.controls.name.value,
    };

    this._rewardsService.createRewardPointsShareToken(tokenData).pipe(
      this._toastService.tapCreating("Share Token")
    ).subscribe(() => {
      this.tokenChanged.next();
      this.form.reset();
    });
  }

  protected delete(token: RewardPointsShareTokenDto) {
    this._rewardsService.deleteRewardPointsShareToken(token.id).pipe(
      this._toastService.tapDeleting("Share Token")
    ).subscribe(() => {
      this.tokenChanged.next();
    });
  }

  protected getOrder(token: RewardPointsShareTokenDto | undefined) {
    const order = Number(token?.id) * -1 + Number.MIN_SAFE_INTEGER / -2;
    if(isNaN(order)) {
      return Number.MIN_SAFE_INTEGER + Number.MIN_SAFE_INTEGER / -2;
    }
    return order;
  }

  protected copyLink(token: RewardPointsShareTokenDto) {
    const link = `${environment.shiftserviceBasePath}/api/v1/reward-points/share/${token.token}`;
    navigator.clipboard.writeText(link).then(() => {
      this._toastService.showSuccess("Link Copied", "The share link has been copied to your clipboard.");
    }).catch(() => {
      this._toastService.showError("Copy Failed", "Failed to copy the link to your clipboard.");
    });
  }
}
