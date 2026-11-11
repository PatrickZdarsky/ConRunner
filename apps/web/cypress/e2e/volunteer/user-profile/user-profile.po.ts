import {BasePage} from '../../../models/base-page';
import {APP_CONFIG} from '../../../config';
import SELECTORS_USER_PROFILE from "./user-profile.selectors";

class UserProfilePage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, 'me');
  }

  verifyAllCardsAreShown() {
    this.verifyTileItemsAreShown(SELECTORS_USER_PROFILE.CARDS);
  }

}

export default new UserProfilePage();
