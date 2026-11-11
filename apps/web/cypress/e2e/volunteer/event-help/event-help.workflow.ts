import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_EVENT_HELP from "./event-help.selectors";
import eventHelpPo from "./event-help.po";

export class EventHelpWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_EVENT_HELP);
  }

  visitEventHelp(eventId: string) {
    eventHelpPo.visitEventHelp(eventId);
  }
}
