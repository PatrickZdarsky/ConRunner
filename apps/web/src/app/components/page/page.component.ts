import {Component, inject, OnInit} from "@angular/core";
import {PageService} from "../../services/page/page.service";
import {AsyncPipe, NgClass} from "@angular/common";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {UserService} from "../../services/user/user.service";
import {combineLatestWith, map} from "rxjs";
import {NotificationService} from "../../services/notification/notification.service";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {icons} from "../../util/icons";
import {Title} from "@angular/platform-browser";

@Component({
  selector: "app-page",
  imports: [
    RouterLink,
    AsyncPipe,
    NgClass,
    FaIconComponent
  ],
  standalone: true,
  templateUrl: "./page.component.html",
  styleUrl: "./page.component.scss"
})
export class PageComponent implements OnInit {

  protected readonly icons = icons;

  private readonly _pageService = inject(PageService);
  private readonly _userService = inject(UserService);
  private readonly _router = inject(Router);
  private readonly _title = inject(Title);
  private readonly _activatedRoute = inject(ActivatedRoute);
  private readonly _notificationService = inject(NotificationService);

  protected get breadcrumbs$() {
    return this._pageService.breadcrumbs$.pipe(
      map(crumb => crumb?.getPath() ?? [])
    );
  }

  protected get pageName$() {
    return this._pageService.pageName$;
  }

  protected get calendarLink$() {
    return this._pageService.calendarLink$.pipe(
      map(link => link === undefined ? undefined : this._router.createUrlTree([link], {relativeTo: this._activatedRoute.firstChild}))
    );
  }

  protected get profile$() {
    return this._userService.userProfile$.pipe(
      map(profile => {
        if (profile === null) {
          return null;
        }

        const volunteer = profile.account.volunteer;
        const displayName = `${volunteer.firstName} ${volunteer.lastName}`.trim() || profile.account.username;

        return {
          displayName,
          imageUrl: profile.account.profile?.trim() || volunteer.profile?.trim() || null,
          initials: this.getInitials(volunteer.firstName, volunteer.lastName, profile.account.username)
        };
      })
    );
  }

  protected get unseenNotificationCount$() {
    return this._notificationService.unreadCount$.pipe(
      map(count => ({count})),
      combineLatestWith(this._userService.userProfile$),
      map(([count, profile]) => profile === null ? null : count)
    );
  }

  ngOnInit() {
    this.pageName$.subscribe(pageName => {
      const title = pageName !== undefined && pageName !== "ShiftControl" ? `ShiftControl | ${pageName}` : "ShiftControl";
      this._title.setTitle(title);
    });
  }

  /**
   * Get initials from profile data
   */
  protected getInitials(firstName?: string, lastName?: string, username?: string) {
    const initials = `${firstName?.charAt(0) ?? ""}${lastName?.charAt(0) ?? ""}`.trim();
    return initials || username?.charAt(0)?.toUpperCase() || "";
  }
}
