import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_EVENTS_LIST from "./events.selectors";
import eventsPo from "./events.po";

export class EventsWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_EVENTS_LIST);
  }

  visitEventsPage() {
    eventsPo.visitPageUrl();
  }
}
