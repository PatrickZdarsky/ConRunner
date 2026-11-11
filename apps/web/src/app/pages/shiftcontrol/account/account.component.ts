import {Component, inject, OnDestroy} from "@angular/core";
import {UserService} from "../../../services/user/user.service";
import {FormBuilder, FormControl, ReactiveFormsModule} from "@angular/forms";
import {InputTextComponent} from "../../../components/inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {NotificationSettingsDto, RoleDto, UserProfileDto, UserProfileEndpointService} from "../../../../shiftservice-client";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../../components/inputs/input-multitoggle/input-multi-toggle.component";
import {BehaviorSubject, catchError, EMPTY, forkJoin, map, pairwise, startWith, Subscription, switchMap} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {ToastService} from "../../../services/toast/toast.service";
import {environment} from "../../../environment";
import {mapValue} from "../../../util/value-maps";

type notificationToggleValue = NotificationSettingsDto.ChannelsEnum | "ALL";

@Component({
  selector: "app-account",
  imports: [
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    InputMultiToggleComponent,
    AsyncPipe
  ],
  templateUrl: "./account.component.html",
  standalone: true,
  styleUrl: "./account.component.scss"
})
export class AccountComponent implements OnDestroy {

  public readonly form;

  protected testOptions: MultiToggleOptions<notificationToggleValue> = [
    {name: "All", value: "ALL"},
    {name: "Email", value: NotificationSettingsDto.ChannelsEnum.Email},
    {name: "Push", value: NotificationSettingsDto.ChannelsEnum.Push}
  ];
  protected readonly notificationFormControls$ = new BehaviorSubject<undefined | Map<
      NotificationSettingsDto.TypeEnum, FormControl<null | notificationToggleValue>
    >>(undefined);
  protected readonly isPlatformAdmin$;
  protected readonly currentUserProfile$;

  private readonly _userService = inject(UserService);
  private readonly _fb = inject(FormBuilder);
  private readonly _userProfileService = inject(UserProfileEndpointService);
  private readonly _toastService = inject(ToastService);

  private readonly _profileSubscription;
  private _notificationChangesSubscription?: Subscription;

  constructor() {
    this.form = this._fb.group({
      notification: this._fb.control<string | null>(null),
      givenName: this._fb.nonNullable.control<string>(""),
      lastName: this._fb.nonNullable.control<string>(""),
      email: this._fb.nonNullable.control<string>(""),
      username: this._fb.nonNullable.control<string>(""),
      checked: this._fb.nonNullable.control<boolean>(false)
    });

    this.isPlatformAdmin$ = this._userService.isPlatformAdmin$;
    this.currentUserProfile$ = this._userService.userProfile$;

    this._profileSubscription = this.currentUserProfile$.subscribe(appProfile => {
      if(appProfile) {
        this.form.setValue({
          givenName: appProfile.account.volunteer.firstName,
          lastName: appProfile.account.volunteer.lastName,
          email: appProfile.account.email,
          username: appProfile.account.username,
          checked: false,
          notification: null
        });
      }
    });

    this._userProfileService.getCurrentUserProfile().subscribe(profile => {
      const notificationControls = new Map(profile.notifications
        .filter(notif => {
          if(notif.type.startsWith("ADMIN_")) {
            return profile.account.platformAdmin === true;
          }

          if(notif.type.startsWith("PLANNER_")){
            return profile.planningPlans.length > 0;
          }

          if(notif.type.startsWith("VOLUNTEER_")){
            return true;
          }

          return false;
        })
        .map(notification => {
          const currentValue = notification.channels.length === 2 ? "ALL" :
            notification.channels.length === 1 ? Array.from(notification.channels)[0] : null;
          const control = this._fb.control<null | notificationToggleValue>(currentValue);
          return [notification.type, control] as const;
        })
        .sort((a,b) => a[0].localeCompare(b[0]))
      );

      const notificationChanges$ = [...notificationControls.entries()].map(([type, control]) => control.valueChanges.pipe(
        startWith(control.value),
        pairwise(),
        switchMap(([oldValue, newValue]) => this._userProfileService.updateNotificationSettings({
          type: type,
          channels: newValue === "ALL" ? [NotificationSettingsDto.ChannelsEnum.Email, NotificationSettingsDto.ChannelsEnum.Push] :
            newValue ? [newValue] : []
        }).pipe(
          this._toastService.tapSaving("Notification Setting", item => this.getNotificationKindName(item.type)),
          catchError(() => {
            // on error, revert the change
            control.setValue(oldValue, {emitEvent: false});
            return [];
          })
        ))
      ));

      this._notificationChangesSubscription = notificationChanges$.length === 0 ?
        undefined :
        forkJoin(notificationChanges$).subscribe();

      this.notificationFormControls$.next(notificationControls);
    });
  }

  ngOnDestroy(): void {
    this._profileSubscription.unsubscribe();
    this._notificationChangesSubscription?.unsubscribe();
  }

  /**
   * open the external keycloak management console
   */
  goToManagement() {
    window.location.href = this._userService.manageUrl;
  }

  signOut() {
    this._userService.logout();
  }

  protected getNotificationKindName(type: NotificationSettingsDto.TypeEnum): string {
    switch (type) {
      case NotificationSettingsDto.TypeEnum.VolunteerAutoAssigned:
        return "Auto Assignments";
      case NotificationSettingsDto.TypeEnum.VolunteerShiftReminder:
        return "Shift Reminders";
      case NotificationSettingsDto.TypeEnum.VolunteerRequestHandled:
        return "Request Updates";
      case NotificationSettingsDto.TypeEnum.VolunteerTradeOrAuction:
        return "Trades & Auctions";
      case NotificationSettingsDto.TypeEnum.VolunteerPlansChanged:
        return "Plan Changes";
      case NotificationSettingsDto.TypeEnum.VolunteerRolesChanged:
        return "Role Changes";
      case NotificationSettingsDto.TypeEnum.VolunteerStatusChanged:
        return "User Status Changes";
      case NotificationSettingsDto.TypeEnum.PlannerVolunteerRequest:
        return "Requests";
      case NotificationSettingsDto.TypeEnum.PlannerVolunteerJoinedPlan:
        return "Volunteer Joined";
      case NotificationSettingsDto.TypeEnum.AdminPlannerJoinedPlan:
        return "Planner Joined";
      case NotificationSettingsDto.TypeEnum.AdminTrustAlertReceived:
        return "Trust Alerts";
      case NotificationSettingsDto.TypeEnum.AdminRewardSyncUsed:
        return "Rewards Synced";
    }
  }

  protected getRoleLabel(role: RoleDto): string {
    return role.description?.trim()
      ? `${role.name}: ${role.description}`
      : role.name;
  }

  protected getCalendarLink(profile: UserProfileDto | null): string | null {
    const shareId = profile?.calendarShareId?.trim();
    if(!shareId) {
      return null;
    }

    const basePath = environment.shiftserviceBasePath.replace(/\/+$/, "");
    return `${basePath}/api/v1/calendar/share/${encodeURIComponent(shareId)}`;
  }

  protected copyCalendarLink(profile: UserProfileDto | null) {
    const link = this.getCalendarLink(profile);

    if(!link) {
      this._toastService.showError("Calendar Link Missing", "The calendar link is not available yet.");
      return;
    }

    navigator.clipboard.writeText(link).then(() => {
      this._toastService.showSuccess("Calendar Link Copied", "The calendar feed link has been copied to your clipboard.");
    }).catch(() => {
      this._toastService.showError("Copy Failed", "Failed to copy the calendar feed link.");
    });
  }

  protected regenerateCalendarLink() {
    this._userProfileService.regenerateCurrentUserCalendarShareId().pipe(
      switchMap(() => this._userService.refreshProfile()),
      this._toastService.tapSuccess(
        "Calendar Link Regenerated",
        () => "The old calendar link no longer works. Update your calendar app with the new link."
      ),
      this._toastService.tapError("Calendar Link Error", mapValue.apiErrorToMessage),
      catchError(() => EMPTY)
    ).subscribe();
  }
}
