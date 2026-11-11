import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class HomePage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "");
  }
}

export default new HomePage();
