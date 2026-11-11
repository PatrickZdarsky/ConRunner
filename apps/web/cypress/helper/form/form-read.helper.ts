import {verifySelectorVisibility} from './form.helper';

/**
 * Retrieves the current value of an input field after ensuring it is visible.
 *
 * @param selector - The selector of the input element.
 * @returns The current value of the input element.
 */
export function getSelectValue(selector: string): Cypress.Chainable<string> {
  verifySelectorVisibility(selector);
  return cy.get(selector).invoke('val');
}

/**
 * Verifies that the displayed value in a mat-select matches the expected string.
 *
 * @param selector - Static ID selector for the <mat-select> element.
 */
export function getMatSelectValue(selector: string): Cypress.Chainable<string> {
  verifySelectorVisibility(selector);
  return cy.get(selector).find('.mat-mdc-select-value-text').invoke('text');
}

/**
 * Verifies the state of a material toggle element by checking its 'aria-checked' attribute.
 *
 * The function scrolls the element into view, ensures it exists and is visible
 *
 * @param {string} selector - The CSS selector for the toggle element.
 */
export function getMaterialToggleState(selector: string): Cypress.Chainable<string> {
  verifySelectorVisibility(selector);
  return cy.get(selector).invoke('attr', 'aria-checked');
}

/**
 * Retrieves the current text content of a WYSIWYG editor.
 *
 * @param selector - The base ID selector of the <app-wysiwyg> component.
 * @returns The current plain text inside the editable content area.
 */
export function getWysiwygValue(selector: string): Cypress.Chainable<string> {
  const contentSelector = selector + " div.ProseMirror[contenteditable='true']";
  return cy.get(contentSelector).invoke('text');
}

/**
 * Returns the trimmed text content of a readonly span element.
 *
 * @param selector - ID selector of the readonly element.
 * @returns The visible text value.
 */
export function getReadonlyValue(selector: string): Cypress.Chainable<string> {
  verifySelectorVisibility(selector);
  return cy
    .get(selector)
    .invoke('text')
    .then((text) => text.trim());
}

export function getXsbTextValue(selector: string) {
  return cy
    .get(selector)
    .invoke('text')
    .then((t) => t.trim());
}

export function getXsbToggleState(selector: string) {
  return cy
    .get(selector)
    .find('input[type="checkbox"]')
    .invoke('prop', 'checked')
    .then((checked) => String(checked)) as Cypress.Chainable<string>;
}

export function getXsbMultiToggleValue(selector: string) {
  return cy
    .get(selector)
    .should('exist')
    .find('button.bg-gradient_badge')
    .should('be.visible')
    .first()
    .invoke('text')
    .then((t) => t.trim());
}
