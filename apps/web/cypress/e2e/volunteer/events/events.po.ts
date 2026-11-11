import {BasePage} from "../../../models/base-page";
import {APP_CONFIG} from "../../../config";

class EventsPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, "events");
  }
}

export default new EventsPage();
