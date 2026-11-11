import { Routes } from "@angular/router";
import { LoginComponent } from "./pages/public/login/login.component";
import { EventsComponent } from "./pages/shiftcontrol/events/events.component";
import { EventComponent } from "./pages/shiftcontrol/event/event.component";
import {
  BC_EVENTS,
  BC_EVENT,
  BC_ACCOUNT,
  BC_HOME,
  BC_SHIFT_CALENDAR,
  BC_SHIFT_DETAILS,
  BC_PLAN_ONBOARDING,
  BC_EVENT_EDIT,
  BC_EVENT_CREATE,
  BC_NOTIFICATIONS,
  BC_EVENT_PLANS,
  BC_EVENT_HELP,
  BC_EVENT_VOLUNTEER_DASHBOARD,
  BC_REWARDS_SYNC,
  BC_PRETALX_SYNC,
  BC_AUDIT_LOG,
  BC_TRUST_ALERTS,
  BC_VOLUNTEER,
  BC_VOLUNTEERS
} from "./breadcrumbs";
import { breadcrumbsGuard } from "./guards/breadcrumbs/breadcrumbs.guard";
import {authenticatedGuard, notLoggedInGuard} from "./guards/keycloak/keycloak.guard";
import {AccountComponent} from "./pages/shiftcontrol/account/account.component";
import {HomeComponent} from "./pages/shiftcontrol/home/home.component";
import {EventCalendarComponent} from "./pages/shiftcontrol/event/event-calendar/event-calendar.component";
import {ShiftDetailsComponent} from "./pages/shiftcontrol/shift-details/shift-details.component";
import {PlanOnboardingComponent} from "./pages/public/plan-onboarding/plan-onboarding.component";
import {NotificationsComponent} from "./pages/shiftcontrol/notifications/notifications.component";
import {ManageShiftPlansComponent} from "./pages/shiftcontrol/event/manage-shift-plans/manage-shift-plans.component";
import {ManageEventComponent} from "./pages/shiftcontrol/event/manage-event/manage-event.component";
import {CreateEventComponent} from "./pages/shiftcontrol/event/create-event/create-event.component";
import {EventHelpComponent} from "./pages/shiftcontrol/event/event-help/event-help.component";
import {VolunteerDashboardComponent} from "./pages/shiftcontrol/event/volunteer-dashboard/volunteer-dashboard.component";
import {RewardsSyncComponent} from "./pages/shiftcontrol/rewards-sync/rewards-sync.component";
import {PretalxSyncComponent} from "./pages/shiftcontrol/pretalx-sync/pretalx-sync.component";
import {AuditLogComponent} from "./pages/shiftcontrol/audit-log/audit-log.component";
import {TrustAlertsComponent} from "./pages/shiftcontrol/trust-alerts/trust-alerts.component";
import {VolunteerComponent} from "./pages/shiftcontrol/volunteer/volunteer.component";
import {ApplicationUsersComponent} from "./pages/shiftcontrol/application-users/application-users.component";
import {isAdminGuard} from "./guards/access/admin.guard";
import {isVolunteerInEventGuard} from "./guards/access/volunteer.guard";
import {isPlannerInEventGuard} from "./guards/access/planner.guard";

export const routes: Routes = ([
  { path: "", component: HomeComponent, pathMatch: "full", data: {breadcrumbs: BC_HOME}, canActivate: [authenticatedGuard] },
  { path: "login", component: LoginComponent, canActivate: [notLoggedInGuard] },
  { path: "me", component: AccountComponent,
    data: {breadcrumbs: BC_ACCOUNT}, canActivate: [authenticatedGuard]},
  { path: "notifications", component: NotificationsComponent,
    data: {breadcrumbs: BC_NOTIFICATIONS}, canActivate: [authenticatedGuard]},

  /* onboarding */
  { path: "join/:shiftPlanInvite", redirectTo: "onboarding/:shiftPlanInvite", pathMatch: "full" },
  { path: "onboarding/:shiftPlanInvite", component: PlanOnboardingComponent,
    data: { breadcrumbs: BC_PLAN_ONBOARDING }},

  /* events */
  { path: "events", component: EventsComponent,
    data: {breadcrumbs: BC_EVENTS}, canActivate: [authenticatedGuard]},
  { path: "events/create", component: CreateEventComponent,
    data: {breadcrumbs: BC_EVENT_CREATE}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "events/:eventId", component: EventComponent,
    data: {breadcrumbs: BC_EVENT}, canActivate: [authenticatedGuard, isVolunteerInEventGuard("eventId")]},
  { path: "events/:eventId/plans", component: ManageShiftPlansComponent,
    data: {breadcrumbs: BC_EVENT_PLANS}, canActivate: [authenticatedGuard, isPlannerInEventGuard("eventId")]},
  { path: "events/:eventId/manage", component: ManageEventComponent,
    data: {breadcrumbs: BC_EVENT_EDIT}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "events/:eventId/calendar", component: EventCalendarComponent,
    data: { breadcrumbs: BC_SHIFT_CALENDAR }, canActivate: [authenticatedGuard, isVolunteerInEventGuard("eventId")]},
  { path: "events/:eventId/help", component: EventHelpComponent,
    data: { breadcrumbs: BC_EVENT_HELP }, canActivate: [authenticatedGuard, isVolunteerInEventGuard("eventId")]},
  { path: "events/:eventId/volunteer", component: VolunteerDashboardComponent,
    data: { breadcrumbs: BC_EVENT_VOLUNTEER_DASHBOARD }, canActivate: [authenticatedGuard, isVolunteerInEventGuard("eventId", false)]},

  /* shifts */
  { path: "shifts/:shiftId", component: ShiftDetailsComponent,
    data: { breadcrumbs: BC_SHIFT_DETAILS }, canActivate: [authenticatedGuard]},

  /* admin */
  { path: "rewards-sync", component: RewardsSyncComponent,
    data: {breadcrumbs: BC_REWARDS_SYNC}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "pretalx-sync", component: PretalxSyncComponent,
    data: {breadcrumbs: BC_PRETALX_SYNC}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "audit", component: AuditLogComponent,
    data: {breadcrumbs: BC_AUDIT_LOG}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "trust", component: TrustAlertsComponent,
    data: {breadcrumbs: BC_TRUST_ALERTS}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "volunteers", component: ApplicationUsersComponent,
    data: {breadcrumbs: BC_VOLUNTEERS}, canActivate: [authenticatedGuard, isAdminGuard]},
  { path: "volunteers/:volunteerId", component: VolunteerComponent,
    data: {breadcrumbs: BC_VOLUNTEER}, canActivate: [authenticatedGuard, isAdminGuard]}

  /* add breadcrumbs guard to each route definition*/
] as Routes).map(
  route => route.redirectTo ? route : ({...route, canActivate: [...(route.canActivate ?? []), breadcrumbsGuard]})
);
