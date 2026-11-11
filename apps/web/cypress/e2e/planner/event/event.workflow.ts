import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_PLANNER_EVENT from "./event.selectors";
import plannerEventPo from "./event.po";

export class PlannerEventWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_PLANNER_EVENT);
  }

  visitPlannerEvent() {
    plannerEventPo.visitEvent(SELECTORS_PLANNER_EVENT.EVENT.id);
  }

  openPlannerDashboard() {
    plannerEventPo.navigateOverCard(SELECTORS_PLANNER_EVENT.CARDS.PLANNER_DASHBOARD, `events/${SELECTORS_PLANNER_EVENT.EVENT.id}/plans`);
  }
}
