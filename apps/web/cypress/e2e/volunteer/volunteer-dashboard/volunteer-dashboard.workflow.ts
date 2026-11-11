import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_VOLUNTEER_DASHBOARD from "./volunteer-dashboard.selectors";
import volunteerDashboardPo from "./volunteer-dashboard.po";

export class VolunteerDashboardWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_VOLUNTEER_DASHBOARD);
  }

  visitVolunteerDashboard(eventId: string) {
    volunteerDashboardPo.visitVolunteerDashboard(eventId);
  }
}
