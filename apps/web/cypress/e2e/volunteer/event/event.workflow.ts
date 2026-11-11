import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_EVENTS from "./event.selectors";
import eventPo from "./event.po";

export class EventWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_EVENTS);
  }

  visitEventPage() {
    eventPo.visitEvent(SELECTORS_EVENTS.EVENT.id);
  }

  openVolunteerDashboard() {
    eventPo.navigateOverCard(SELECTORS_EVENTS.CARDS.VOLUNTEER_DASHBOARD, `events/${SELECTORS_EVENTS.EVENT.id}/volunteer`);
  }

  openEventCalendar() {
    eventPo.navigateOverCard(SELECTORS_EVENTS.CARDS.EVENT_CALENDAR, `events/${SELECTORS_EVENTS.EVENT.id}/calendar`);
  }

  openEventHelp() {
    eventPo.navigateOverCard(SELECTORS_EVENTS.CARDS.HELP, `events/${SELECTORS_EVENTS.EVENT.id}/help`);
  }

}
