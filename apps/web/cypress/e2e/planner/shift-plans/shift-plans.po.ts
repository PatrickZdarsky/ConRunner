import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class PlannerShiftPlansPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }

  visitPlannerDashboard(eventId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}events/${eventId}/plans`);
  }
}

export default new PlannerShiftPlansPage();
