import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_SHIFT_PLAN from "./shift-plan.selectors";
import shiftPlanPo from "./shift-plan.po";

export class ShiftPlanWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_SHIFT_PLAN);
  }

  visitInvite(code: string) {
    shiftPlanPo.visitInvite(code);
  }
}
