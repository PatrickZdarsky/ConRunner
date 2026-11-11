import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class PlannerEventPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }

  visitEvent(eventId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}events/${eventId}`);
  }
}

export default new PlannerEventPage();
