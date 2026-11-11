import LoginPage from './login.po';
import SELECTORS_LOGIN from './login.selectors';
import {BaseWorkflow} from '../../../models/base.workflow';
import {APP_CONFIG} from '../../../config';
import {clickElement} from "../../../helper/form/form.helper";

export class LoginWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_LOGIN);
  }

  public visitLoginPage(): void {
    LoginPage.visitLoginPage();
  }

  public loginViaKeycloak(username: string, password: string): void {
    clickElement(SELECTORS_LOGIN.LOGIN.submit as string, false)
    cy.origin(
      APP_CONFIG.KEYCLOAK_URL,
      {
        args: {username, password, LOGIN_SELECTORS: SELECTORS_LOGIN},
      },
      ({username: u, password: p, LOGIN_SELECTORS}) => {
        cy.url({timeout: 500}).should('include', 'protocol/openid-connect/auth');
        cy.get(LOGIN_SELECTORS.IDP.usernameInput).clear();
        cy.get(LOGIN_SELECTORS.IDP.usernameInput).type(u);
        cy.get(LOGIN_SELECTORS.IDP.passwordInput).clear();
        cy.get(LOGIN_SELECTORS.IDP.passwordInput).type(p);
        cy.get(LOGIN_SELECTORS.IDP.loginButton).click();
      },
    );
  }

  public loginRedirect(): void {
    cy.clearAllCookies();
    cy.clearAllLocalStorage();
    cy.visit(APP_CONFIG.BASE_URL);
    cy.url({timeout: APP_CONFIG.TIMEOUT_S}).should('include', 'welcome');
  }

  public logout(): void {
    this.clickById('clientMenuButton');
    this.clickById('logout');
    cy.clearLocalStorage();
    cy.wait(APP_CONFIG.TIMEOUT_S);
  }

}
