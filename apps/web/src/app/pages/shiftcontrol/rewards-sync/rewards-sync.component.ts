import {Component, inject} from "@angular/core";
import { icons } from "../../../util/icons";
import {RewardPointsEndpointService, RewardPointsShareTokenDto} from "../../../../shiftservice-client";
import {BehaviorSubject,} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {ManageRewardsShareTokenComponent} from "../../../components/manage-rewards-share-token/manage-rewards-share-token.component";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {ToastService} from "../../../services/toast/toast.service";
import {mapValue} from "../../../util/value-maps";

@Component({
  selector: "app-rewards-share",
  imports: [
    AsyncPipe,
    ManageRewardsShareTokenComponent,
    InputButtonComponent
  ],
  templateUrl: "./rewards-sync.component.html",
  styleUrl: "./rewards-sync.component.scss"
})
export class RewardsSyncComponent {

  protected readonly shares$ = new BehaviorSubject<RewardPointsShareTokenDto[]>([]);
  protected readonly icons = icons;

  private readonly _rewardsService = inject(RewardPointsEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.fetchTokens();
  }

  protected fetchTokens() {
    this._rewardsService.getAllRewardPointsShareTokens().subscribe(res => this.shares$.next(res));
  }

  protected downloadBalances() {
    this._rewardsService.exportRewardPoints().pipe(
      this._toastService.tapSuccess("Downloaded Reward Point Balances"),
      this._toastService.tapError("Reward points download failed", mapValue.apiErrorToMessage)
    ).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "reward_points_export.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    });
  }

}
