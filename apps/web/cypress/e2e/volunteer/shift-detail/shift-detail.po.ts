import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class ShiftDetailPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, 'shifts');
  }

  visitShift(shiftId: string) {
    cy.visit(`${APP_CONFIG.BASE_URL}shifts/${shiftId}`);
  }
}

export default new ShiftDetailPage();
