import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class EventHelpPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }

  visitEventHelp(eventId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}events/${eventId}/help`);
  }
}

export default new EventHelpPage();
