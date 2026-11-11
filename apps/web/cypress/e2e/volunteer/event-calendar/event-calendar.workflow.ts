import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_EVENT_CALENDAR from "./event-calendar.selectors";
import eventCalendarPo from "./event-calendar.po";

export class EventCalendarWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_EVENT_CALENDAR);
  }

  visitEventCalendar(eventId: string) {
    eventCalendarPo.visitEventCalendar(eventId);
  }
}
