import {BasePage} from '../../../models/base-page';
import {APP_CONFIG} from '../../../config';

class LoginPage extends BasePage {
  constructor() {
    super(APP_CONFIG.BASE_URL, '');
  }

  visitLoginPage(): void {
    this.visitBaseUrl();
  }
}

export default new LoginPage();
