import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_HOME from "./home.selectors";
import homePo from "./home.po";

export class HomeWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_HOME);
  }

  visitHomePage() {
    homePo.visitPageUrl();
  }
}
