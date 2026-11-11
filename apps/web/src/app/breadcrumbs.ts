import {Breadcrumb} from "./util/breadcrumb";

/* note: name and href should be customized by the component specifically to selected path, if applicable */
export const BC_HOME = new Breadcrumb("ShiftControl", "/");
export const BC_ACCOUNT = new Breadcrumb("Account", "/account", BC_HOME);
export const BC_NOTIFICATIONS = new Breadcrumb("Notifications", "/notifications", BC_HOME);

export const BC_REWARDS_SYNC = new Breadcrumb("Reward Points Sync", "/rewards-sync", BC_HOME);
export const BC_PRETALX_SYNC = new Breadcrumb("Pretalx Sync", "/pretalx-sync", BC_HOME);
export const BC_AUDIT_LOG = new Breadcrumb("Audit Log", "/audit", BC_HOME);
export const BC_TRUST_ALERTS = new Breadcrumb("Trust Alerts", "/trust", BC_HOME);
export const BC_VOLUNTEERS = new Breadcrumb("Volunteers", "/volunteers", BC_HOME);
export const BC_VOLUNTEER = new Breadcrumb("Volunteer", "volunteer-id", BC_VOLUNTEERS);

export const BC_EVENTS = new Breadcrumb("Events", "/events", BC_HOME);
export const BC_EVENT = new Breadcrumb("Event", "event-id", BC_EVENTS);
export const BC_EVENT_CREATE = new Breadcrumb("Create Event", "create", BC_EVENTS);
export const BC_EVENT_EDIT = new Breadcrumb("Manage", "manage", BC_EVENT);
export const BC_EVENT_PLANS = new Breadcrumb("Shift Plans", "plans", BC_EVENT);
export const BC_EVENT_VOLUNTEER_DASHBOARD = new Breadcrumb("Volunteer", "volunteer", BC_EVENT);
export const BC_EVENT_HELP = new Breadcrumb("Help", "help", BC_EVENT);

export const BC_PLAN_ONBOARDING = new Breadcrumb("Onboarding", "onboarding", BC_EVENT);
export const BC_SHIFT_CALENDAR = new Breadcrumb("Calendar", "calendar", BC_EVENT);
export const BC_SHIFT_DETAILS = new Breadcrumb("Shift Details", "shift-id", BC_EVENT);
