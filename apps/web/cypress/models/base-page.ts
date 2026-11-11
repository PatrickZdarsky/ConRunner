import {expectPageState, PAGE_STATE, PageState} from '../helper/common/page-state.helper';
import {APP_CONFIG} from '../config';
import SELECTORS_LOGIN from '../e2e/basic/login/login.selectors';

export abstract class BasePage {
  private readonly baseURL: string;
  private readonly pageURL: string;
  private readonly T: number = APP_CONFIG.TIMEOUT_L;

  protected constructor(baseURL: string, pageUrl: string) {
    this.baseURL = baseURL;
    this.pageURL = pageUrl;
  }

  public getPageUrl(): string {
    return this.pageURL;
  }

  public visitBaseUrl(): void {
    cy.visit(APP_CONFIG.BASE_URL);
    cy.wait(APP_CONFIG.TIMEOUT_S);
  }

  public visitPageUrl(expected: PageState = PAGE_STATE.OK): void {
    const url = this.baseURL + this.pageURL;
    cy.visit(url, { timeout: this.T });
    this.verifyPageUrl(expected);
  }

  public verifyPageUrl(expected: PageState = PAGE_STATE.OK): void {
    const url = this.baseURL + this.pageURL;
    const expectedPath = new URL(url).pathname;

    if (expected !== PAGE_STATE.OK) {
      cy.location('pathname', { timeout: this.T }).should('not.eq', expectedPath);
      expectPageState(expected, this.T);
      return;
    }
    cy.document({ timeout: this.T }).its('readyState').should('eq', 'complete');

    cy.location('pathname', { timeout: this.T }).then((p) => {
      if (p !== expectedPath) cy.visit(url, { timeout: this.T });
    });

    cy.get('body *', { timeout: this.T }).should('have.length.greaterThan', APP_CONFIG.MIN_DOM);
    cy.get('app-root', { timeout: this.T }).should('be.visible');
    cy.location('pathname', { timeout: this.T }).should('eq', expectedPath);
    cy.url({ timeout: this.T }).should('include', this.pageURL);

    Object.values(SELECTORS_LOGIN.ERROR_STATE_COMPONENTS).forEach((s) => cy.get(s).should('not.exist'));
  }

  public verifyTileItemsAreShown(tile: Record<string, string>): void {
    Object.values(tile).forEach(selector => {
      cy.contains('h1, h2, h3, h4', selector).should('exist');
    });
  }

  public navigateOverCards(...steps: Array<[string, BasePage]>) {
      for (const [heading, page] of steps) {
        cy.contains('.card', heading).click();
        cy.location('pathname').should('eq', "/" + page.getPageUrl());
      }
    }

  public navigateOverCard(heading: string, param: BasePage | string): void {
    if (typeof param === 'string') {
      cy.contains('.card', heading).click();
      cy.location('pathname').should('eq', param.startsWith('/') ? param : '/' + param);
    } else {
      cy.contains('.card', heading).click();
      cy.location('pathname').should('eq', '/' + param.getPageUrl());
    }
  }
}
