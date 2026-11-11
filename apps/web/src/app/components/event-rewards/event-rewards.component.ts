import {Component, inject, Input} from "@angular/core";
import {EventShiftPlansOverviewDto} from "../../../shiftservice-client";
import {BehaviorSubject} from "rxjs";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {icons} from "../../util/icons";
import {AsyncPipe} from "@angular/common";
import {ToastService} from "../../services/toast/toast.service";

@Component({
  selector: "app-event-rewards",
  imports: [
    FaIconComponent,
    AsyncPipe
  ],
  standalone: true,
  templateUrl: "./event-rewards.component.html",
  styleUrl: "./event-rewards.component.scss"
})
export class EventRewardsComponent {
  protected readonly icons = icons;
  protected eventDashboard$ = new BehaviorSubject<undefined | EventShiftPlansOverviewDto>(undefined);

  private readonly _toastService = inject(ToastService);

  @Input()
  public set eventDashboard(value: EventShiftPlansOverviewDto) {
    this.eventDashboard$.next(value);
  }

  protected goToRewards(event: EventShiftPlansOverviewDto){
    if(event.eventOverview.rewardPointsRedeemUrl !== undefined) {
      window.open(event.eventOverview.rewardPointsRedeemUrl, "_blank");
    } else {
      this._toastService.showInfo("Stay tuned..", "There are no rewards yet. Event organizers will add them when they're ready!");
    }
  }
}
