import {Component, inject} from "@angular/core";
import {NotificationService} from "../../../services/notification/notification.service";
import {map, tap} from "rxjs";
import {AsyncPipe, DatePipe} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {icons} from "../../../util/icons";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {Router, RouterLink} from "@angular/router";

@Component({
  selector: "app-notifications",
  imports: [
    AsyncPipe,
    FaIconComponent,
    InputButtonComponent,
    DatePipe,
    RouterLink
  ],
  templateUrl: "./notifications.component.html",
  styleUrl: "./notifications.component.scss"
})
export class NotificationsComponent {

  protected readonly icons = icons;
  protected readonly notifications$;

  private readonly _notificationService = inject(NotificationService);
  private readonly _router = inject(Router);

  constructor() {
    this.notifications$ = this._notificationService.notifications$.pipe(
      tap(async () => await this._notificationService.markAllAsRead()),
      map(notifications => [...notifications]),
      map(notifications => notifications
        .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
        .map(notification => ({
        notification,
        tree: notification.url === undefined || notification.url === null ? undefined : this._router.parseUrl(notification.url)
      })))
    );
  }

  protected clearNotification(notificationId: string) {
    this._notificationService.clearNotification(notificationId);
  }

  protected clearHistory() {
    this._notificationService.clearHistory();
  }
}
