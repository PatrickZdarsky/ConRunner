import {Component, inject} from "@angular/core";
import { icons } from "../../../util/icons";
import {
  PretalxApiKeyDetailsDto,
  PretalxEndpointService
} from "../../../../shiftservice-client";
import {BehaviorSubject,} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {ManagePretalxTokenComponent} from "../../../components/manage-pretalx-token/manage-pretalx-token.component";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {ToastService} from "../../../services/toast/toast.service";
import {mapValue} from "../../../util/value-maps";

@Component({
  selector: "app-pretalx-sync",
  imports: [
    AsyncPipe,
    ManagePretalxTokenComponent,
    InputButtonComponent
  ],
  templateUrl: "./pretalx-sync.component.html",
  styleUrl: "./pretalx-sync.component.scss"
})
export class PretalxSyncComponent {

  protected configs$ = new BehaviorSubject<PretalxApiKeyDetailsDto[]>([]);

  protected readonly icons = icons;

  private readonly _pretalxService = inject(PretalxEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.fetchConfigs();
  }

  protected fetchConfigs() {
    this._pretalxService.getPretalxApiKeys().subscribe(res => this.configs$.next(res));
  }

  protected syncNow() {
    this._pretalxService.syncPretalxNow().pipe(
      this._toastService.tapSuccess("Pretalx synchronization started",() => "Events will be updated in the background."),
      this._toastService.tapError("Pretalx synchronization failed", mapValue.apiErrorToMessage)
    ).subscribe();
  }
}
