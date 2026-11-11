import {BaseWorkflow} from './base.workflow';
import {LoginWorkflow} from '../e2e/basic/login/login.workflow';
import {UserProfileWorkflow} from "../e2e/volunteer/user-profile/user-profile.workflow";
import {ShiftDashBoardWorkflow} from "../e2e/volunteer/shift-dashboard/shift-dashboard.workflow";
import {EventWorkflow} from "../e2e/volunteer/event/event.workflow";
import {ShiftDetailWorkflow} from "../e2e/volunteer/shift-detail/shift-detail.workflow";
import {ShiftPlanWorkflow} from "../e2e/volunteer/shift-plan/shift-plan.workflow";
import {HomeWorkflow} from "../e2e/volunteer/home/home.workflow";
import {EventsWorkflow} from "../e2e/volunteer/events/events.workflow";
import {EventCalendarWorkflow} from "../e2e/volunteer/event-calendar/event-calendar.workflow";
import {VolunteerDashboardWorkflow} from "../e2e/volunteer/volunteer-dashboard/volunteer-dashboard.workflow";
import {NotificationsWorkflow} from "../e2e/volunteer/notifications/notifications.workflow";
import {EventHelpWorkflow} from "../e2e/volunteer/event-help/event-help.workflow";
import {PlannerEventWorkflow} from "../e2e/planner/event/event.workflow";
import {PlannerShiftPlansWorkflow} from "../e2e/planner/shift-plans/shift-plans.workflow";

const registry: Record<string, () => BaseWorkflow> = {};

export class WorkflowFactory {
  static register(name: string, factory: () => BaseWorkflow): void {
    registry[name] = factory;
  }

  static get<T extends BaseWorkflow>(name: string): T {
    const factory = registry[name];
    if (!factory) throw new Error(`No workflow registered for: ${name}`);
    return factory() as T;
  }
}

WorkflowFactory.register('login', () => new LoginWorkflow());
WorkflowFactory.register('userProfile', () => new UserProfileWorkflow());
WorkflowFactory.register('shiftDashboard', () => new ShiftDashBoardWorkflow());
WorkflowFactory.register('shiftPlans', () => new ShiftPlanWorkflow());
WorkflowFactory.register('shiftDetail', () => new ShiftDetailWorkflow());
WorkflowFactory.register('events', () => new EventWorkflow());
WorkflowFactory.register('home', () => new HomeWorkflow());
WorkflowFactory.register('eventsList', () => new EventsWorkflow());
WorkflowFactory.register('eventCalendar', () => new EventCalendarWorkflow());
WorkflowFactory.register('volunteerDashboard', () => new VolunteerDashboardWorkflow());
WorkflowFactory.register('notifications', () => new NotificationsWorkflow());
WorkflowFactory.register('eventHelp', () => new EventHelpWorkflow());
WorkflowFactory.register('plannerEvent', () => new PlannerEventWorkflow());
WorkflowFactory.register('plannerShiftPlans', () => new PlannerShiftPlansWorkflow());
