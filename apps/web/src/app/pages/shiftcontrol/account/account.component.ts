/* eslint-disable @typescript-eslint/member-ordering -- injected dependencies initialize fields below */
import {Component, inject, OnDestroy} from "@angular/core";
import {UserService} from "../../../services/user/user.service";
import {FormBuilder, FormControl, ReactiveFormsModule} from "@angular/forms";
import {InputTextComponent} from "../../../components/inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {
  EmailPreferenceEndpointService, InAppPreferenceEndpointService, NotificationPreferenceEndpointService,
  UserProfileDto, UserProfileEndpointService
} from "../../../../api-client";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../../components/inputs/input-multitoggle/input-multi-toggle.component";
import {BehaviorSubject, catchError, concatMap, EMPTY, forkJoin, Subscription, switchMap} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {ToastService} from "../../../services/toast/toast.service";
import {environment} from "../../../environment";
import {mapValue} from "../../../util/value-maps";

type notificationToggleValue = "ALL" | "EMAIL" | "PUSH";
@Component({
  selector: "app-account",
  imports: [InputTextComponent, ReactiveFormsModule, TypedFormControlDirective, InputButtonComponent, InputMultiToggleComponent, AsyncPipe],
  templateUrl: "./account.component.html", standalone: true, styleUrl: "./account.component.scss"
})
export class AccountComponent implements OnDestroy {
  private readonly _userService = inject(UserService);
  private readonly _fb = inject(FormBuilder);
  private readonly _userProfileService = inject(UserProfileEndpointService);
  private readonly preferences = inject(NotificationPreferenceEndpointService);
  private readonly email = inject(EmailPreferenceEndpointService);
  private readonly inApp = inject(InAppPreferenceEndpointService);
  private readonly _toastService = inject(ToastService);
  private readonly subscriptions = new Subscription();
  private readonly labels = new Map<string, string>();
  public readonly form = this._fb.nonNullable.group({givenName: "", email: "", username: ""});
  protected readonly testOptions: MultiToggleOptions<notificationToggleValue> = [
    {name: "All", value: "ALL"}, {name: "Email", value: "EMAIL"}, {name: "Push", value: "PUSH"}
  ];
  protected readonly notificationFormControls$ =
    new BehaviorSubject<Map<string, FormControl<notificationToggleValue | null>> | undefined>(undefined);
  protected readonly isPlatformAdmin$ = this._userService.isPlatformAdmin$;
  protected readonly currentUserProfile$ = this._userService.userProfile$;
  constructor() {
    this.subscriptions.add(this.currentUserProfile$.subscribe(profile => {
      if (profile) {
        this.form.setValue({givenName: profile.account.volunteer.name, email: profile.account.email, username: profile.account.username});
      }
    }));
    this.subscriptions.add(this.preferences.listNotificationPreferences().subscribe(items => {
      const controls = new Map<string, FormControl<notificationToggleValue | null>>();
      for (const item of items) {
        if (!item.type) {continue;}
        const type = item.type;
        this.labels.set(type, item.label ?? type);
        const initial = item.email && item.inApp ? "ALL" : item.email ? "EMAIL" : item.inApp ? "PUSH" : null;
        const control = this._fb.control<notificationToggleValue | null>(initial);
        controls.set(type, control);
        this.subscriptions.add(control.valueChanges.pipe(concatMap(value => forkJoin({
          email: this.email.updateEmailPreference(type, {enabled: value === "ALL" || value === "EMAIL"}),
          inApp: this.inApp.updateInAppPreference(type, {enabled: value === "ALL" || value === "PUSH"})
        }).pipe(catchError(() => {
          this._toastService.showError("Notification settings", "Could not save the setting. Reload the page to see its current value.");
          return EMPTY;
        })))).subscribe());
      }
      this.notificationFormControls$.next(controls);
    }));
  }
  ngOnDestroy(): void { this.subscriptions.unsubscribe(); }
  goToManagement() { window.location.href = this._userService.manageUrl; }
  signOut() { this._userService.logout(); }
  protected getNotificationKindName(type: string): string { return this.labels.get(type) ?? type; }
  protected getCalendarLink(profile: UserProfileDto | null): string | null {
    const shareId = profile?.calendarShareId?.trim();
    if(!shareId) {
      return null;
    }

    const basePath = environment.apiBasePath.replace(/\/+$/, "");
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
