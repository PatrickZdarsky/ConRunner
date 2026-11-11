import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class EventCalendarPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }

  visitEventCalendar(eventId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}events/${eventId}/calendar`);
  }
}

export default new EventCalendarPage();
