import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class NotificationsPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "notifications");
  }
}

export default new NotificationsPage();
