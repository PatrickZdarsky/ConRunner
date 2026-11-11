import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class VolunteerDashboardPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }

  visitVolunteerDashboard(eventId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}events/${eventId}/volunteer`);
  }
}

export default new VolunteerDashboardPage();
