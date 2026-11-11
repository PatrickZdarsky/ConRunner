import {BaseWorkflow} from '../../../models/base.workflow';
import SELECTORS_USER_PROFILE from "./user-profile.selectors";
import userProfilePo from "./user-profile.po";

export class UserProfileWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_USER_PROFILE);
  }

  visitUserSettingsPage() {
    userProfilePo.visitPageUrl();
  }

  verifyUserSettingsPage() {
    userProfilePo.verifyPageUrl()
  }

  verifyAllTilesAreShown() {
    userProfilePo.verifyAllCardsAreShown()
  }

  navigateFromHeader() {
    cy.get(".header .profile").should("be.visible").click();
    userProfilePo.verifyPageUrl();
  }
}
