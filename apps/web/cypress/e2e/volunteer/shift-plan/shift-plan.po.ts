import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class ShiftPlanPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, 'onboarding');
  }

  visitInvite(code: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}onboarding/${code}`);
  }
}

export default new ShiftPlanPage();
