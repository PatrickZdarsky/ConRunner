import {getElementType} from './form-types.helper';
import {
  setAutocompleteValue,
  setCheckboxValue,
  setDatepickerValue,
  setMaterialToggleState,
  setMatSelectId,
  setMatSelectValue,
  setSelectValue,
  setTextInputValue,
  setWysiwygValue,
} from './form-input.helper';
import {getMaterialToggleState, getMatSelectValue, getReadonlyValue, getSelectValue, getWysiwygValue, getXsbMultiToggleValue, getXsbTextValue, getXsbToggleState} from './form-read.helper';
import {APP_CONFIG} from '../../config';

type InputHandler = (selector: string, value: string | boolean) => void;
type VerifyHandler = (selector: string, expected: string | boolean) => Cypress.Chainable<string>;

const inputHandlers: Record<string, InputHandler> = {
  text: (selector, value) => setTextInputValue(selector, String(value)),
  autocomplete: (selector, value) => setAutocompleteValue(selector, String(value)),
  select: (selector, value) => setSelectValue(selector, String(value)),
  'mat-select': (selector, value) => {
    const v = String(value);
    if (v.startsWith('#')) setMatSelectId(selector, v);
    else setMatSelectValue(selector, v);
  },
  'material-toggle': (selector, value) => setMaterialToggleState(selector, Boolean(value)),
  'mat-checkbox': (selector, value) => setCheckboxValue(selector, Boolean(value)),
  wysiwyg: (selector, value) => setWysiwygValue(selector, String(value)),
  button: (selector) => {
    cy.get(selector).click();
  },
  datepicker: (selector, value) => setDatepickerValue(selector, String(value)),
  'xsb-multi-toggle': (selector, value) => {
    const expected = String(value);
    cy.get(selector).find('button').contains(expected).click();
  },
};

const verifyHandlers: Record<string, VerifyHandler> = {
  text: (selector, expected) => getSelectValue(selector).should('eq', String(expected)),
  autocomplete: (selector, expected) => getSelectValue(selector).should('contain', String(expected)),
  select: (selector, expected) => getSelectValue(selector).should('eq', String(expected)),
  'mat-select': (selector, expected) => getMatSelectValue(selector).should('eq', String(expected)),
  'material-toggle': (selector, expected) => getMaterialToggleState(selector).should('eq', String(expected)),
  wysiwyg: (selector, expected) => getWysiwygValue(selector).should('eq', String(expected)),
  span: (selector, expected) => getReadonlyValue(selector).should('eq', String(expected)),
  'xsb-text':    (selector, expected) => getXsbTextValue(selector).should('eq', String(expected)),
  'xsb-toggle': (selector, expected) => getXsbToggleState(selector).then(actual => String(actual)).should('eq', String(expected)),
  'xsb-multi-toggle': (selector, expected) => getXsbMultiToggleValue(selector).should('eq', String(expected)),
};

/**
 * Handles user input for various types of form elements.
 */
export function handleElementInput(selector: string, value: string | boolean): void {
  cy.then(() => getElementType(selector)).then((type) => {
    cy.log('input type: ' + type + ' | selector: ' + selector);
    const handler = inputHandlers[type];
    if (!handler) throw new Error('Unknown element type for selector:' + selector);
    handler(selector, value);
  });
}

/**
 * Verifies the value of a given element based on its type.
 */
export function verifyElementValue(selector: string, expected: string | boolean): void {
  cy.then(() => getElementType(selector)).then((type) => {
    cy.log('Verifying' + selector + 'as' + type);
    const verifier = verifyHandlers[type];
    if (!verifier) throw new Error('Cannot verify unknown type for selector:' + selector);
    verifier(selector, expected);
  });
}

export function clickElement(selector: string, waitForLoading: boolean = true): void {
  getElementType(selector).then((t) => {
    cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).scrollIntoView();
    cy.get(selector).should('be.visible').and('not.be.disabled');
    if (t === 'checkbox') {
      cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).check({ force: true });
    } else {
      cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).click();
    }
    if (!waitForLoading) {
      return;
    }
    cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_L }).should('not.have.class', 'loading');
  });
}

export function clickAndWaitForPath(selector: string, expectedPathPart: string): void {
  cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).scrollIntoView();
  clickElement(selector, false);
  cy.location('pathname', { timeout: APP_CONFIG.TIMEOUT_L }).should('include', expectedPathPart);
}

export function clickFrontDialog(selector: string): void {
  cy.get('mat-dialog-container', { timeout: APP_CONFIG.TIMEOUT_M })
    .last()
    .within(() => {
      cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).last().scrollIntoView();
      cy.get(selector).last().click();
    });
}

/**
 * Clicks on an element within a specific component.
 *
 * This method is typically used when multiple elements share the same ID or selector
 * but can be distinguished by their surrounding context (the parent component).
 *
 * @param selector - The CSS selector of the element to click
 * @param componentSelector - The selector of the parent component
 * @param expected - The expected text or state used to verify the correct element was selected
 */
export function clickElementByComponent(selector: string, componentSelector: string, expected: string): void {
  const expectedVal = expected ? 'true' : 'false';
  cy.get(componentSelector).within(() => {
    cy.get(selector)
      .invoke('attr', 'aria-checked')
      .then((currentVal) => {
        if (currentVal !== expectedVal) {
          cy.get(selector).click();
        }
        cy.get(selector).invoke('attr', 'aria-checked').should('eq', expectedVal);
      });
  });
}

/**
 * Verifies that a selector exists, is visible, and not disabled.
 *
 * @param selector - The ID selector of the element to check.
 */
export function verifySelectorVisibility(selector: string): void {
  cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_S }).scrollIntoView();
  cy.get(selector).should('exist').should('be.visible').should('not.be.disabled');
}

/**
 * Assert element exists, is visible, and not disabled.
 * @param {string} selector CSS selector.
 * @param deactivated - boolean value, true if the button is deactivated
 */
export function assertEnabledVisible(selector: string, deactivated?: boolean): void {
  cy.get(selector).scrollIntoView();
  cy.get(selector).should('exist');
  cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).and('be.visible');

  if (deactivated) {
    cy.get(selector).should('have.attr', 'aria-checked', 'true');
  } else {
    cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).and('not.be.disabled');
  }
}
