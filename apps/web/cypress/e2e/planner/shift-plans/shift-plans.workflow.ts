import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_PLANNER_SHIFT_PLANS from "./shift-plans.selectors";
import plannerShiftPlansPo from "./shift-plans.po";

export class PlannerShiftPlansWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_PLANNER_SHIFT_PLANS);
  }

  visitPlannerDashboard(eventId: string) {
    plannerShiftPlansPo.visitPlannerDashboard(eventId);
  }
}
