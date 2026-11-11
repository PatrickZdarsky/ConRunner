import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_SHIFT_DETAIL from "./shift-detail.selectors";
import shiftDetailPo from "./shift-detail.po";

export class ShiftDetailWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_SHIFT_DETAIL);
  }

  visitShift(shiftId: string) {
    shiftDetailPo.visitShift(shiftId);
  }
}
