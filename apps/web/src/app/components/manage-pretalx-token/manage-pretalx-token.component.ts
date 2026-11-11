import {Component, inject, Input, Output} from "@angular/core";
import {
  PretalxApiKeyDetailsDto, PretalxApiKeyDto, PretalxEndpointService
} from "../../../shiftservice-client";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {AsyncPipe, NgClass} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {BehaviorSubject, Subject} from "rxjs";

@Component({
  selector: "app-manage-pretalx-token",
  imports: [
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    NgClass,
    DialogComponent,
    AsyncPipe
  ],
  templateUrl: "./manage-pretalx-token.component.html",
  styleUrl: "./manage-pretalx-token.component.scss"
})
export class ManagePretalxTokenComponent {

  @Output()
  public tokenChanged = new Subject<void>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly manageData$ =
    new BehaviorSubject<undefined | { token: PretalxApiKeyDetailsDto | undefined }>(undefined);

  protected showDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _pretalxService = inject(PretalxEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      token: this._fb.nonNullable.control<string>("", [Validators.required]),
      host: this._fb.nonNullable.control<string>("", [Validators.required])
    });
  }

  @Input()
  public set manageData(data: { token: PretalxApiKeyDetailsDto | undefined } | undefined) {
    this.manageData$.next(data);

    this.form.reset();

    if(data !== undefined && data.token !== undefined) {
      this.form.controls.token.setValue(data.token.apiKey);
    }
  }

  protected create() {
    this.form.markAllAsTouched();

    if(this.form.invalid) {
      this._toastService.showError("Invalid Pretalx Config", "Please provide valid pretalx configuration.");
      return;
    }

    const tokenData: PretalxApiKeyDto = {
      apiKey: this.form.controls.token.value,
      pretalxHost: this.form.controls.host.value
    };

    this._pretalxService.addPretalxApiKey(tokenData).pipe(
      this._toastService.tapCreating("Pretalx Sync Configuration")
    ).subscribe(() => {
      this.tokenChanged.next();
      this.form.reset();
    });
  }

  protected delete(token: PretalxApiKeyDetailsDto) {
    this._pretalxService.removePretalxApiKey(token.apiKey).pipe(
      this._toastService.tapDeleting("Share Token")
    ).subscribe(() => {
      this.tokenChanged.next();
    });
  }
}
