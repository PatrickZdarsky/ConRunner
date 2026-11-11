
import { resolvePlaceholders } from '../date/date-value-placeholder.helper';
import { verifySelectorVisibility } from './form.helper';
import {APP_CONFIG} from '../../config';

/**
 * Sets the value of a text input field.
 *
 * Ensures that the input is visible before interacting with it.
 * Clears any existing value
 *
 * @param {string} selector - selector of the input field.
 * @param {string} value - The text value to enter into the input field.
 */
export function setTextInputValue(selector: string, value: string): void {
  verifySelectorVisibility(selector);
  cy.get(selector).clear();
  cy.get(selector).type(value);
}

/**
 * Sets the value
 *
 * Ensures that the input is visible before interacting with it.
 *
 * @param {string} selector - selector of the input field.
 * @param {string} value - The text value to enter into the input field.
 */
export function setSelectValue(selector: string, value: string): void {
  verifySelectorVisibility(selector);
  cy.get(selector).select(value);
  cy.get(selector)
    .find('option:selected')
    .invoke('val')
    .then((selectedVal) => {
      cy.get(selector).should('have.value', selectedVal);
    });
}

/**
 * Selects a value from any <mat-select> by visible text.
 * Works project-wide, only with static ID selectors.
 *
 * @param selector - Static ID selector of the <mat-select>
 * @param value - Visible text to select (e.g. "Employee")
 */
export function setMatSelectValue(selector: string, value: string): void {
  verifySelectorVisibility(selector);
  cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_S }).click();
  cy.get('mat-option', { timeout: APP_CONFIG.TIMEOUT_S }).contains(value).click();
  cy.get('[role="listbox"]', { timeout: APP_CONFIG.TIMEOUT_S }).should('not.exist');
}

/**
 * Selects an Angular Material <mat-select> option by fixed option ID.
 * @param {string} selectId  CSS ID of the mat-select trigger (e.g., '#select-vat_type')
 * @param {string} optionId  CSS ID of the option inside the overlay panel (e.g., '#optionNET')
 */
export function setMatSelectId(selectId: string, optionId: string): void {
  verifySelectorVisibility(selectId);
  cy.get(selectId, { timeout: APP_CONFIG.TIMEOUT_S }).click({ force: true });
  cy.get(selectId, { timeout: APP_CONFIG.TIMEOUT_S })
    .invoke('attr', 'aria-controls')
    .then((id) => {
      cy.get('#' + id)
        .find(optionId)
        .click({ force: true });
    });
  cy.get('[role="listbox"]', { timeout: APP_CONFIG.TIMEOUT_S }).should('not.exist');
}

/**
 * Sets the state of a material toggle (switch) to the expected value.
 *
 * Ensures the toggle exists and is visible before interacting with it.
 * If the current state does not match the expected state, it clicks the toggle to change its value.
 * After toggling, it verifies that the state has been updated correctly.
 *
 * @param {string} selector - selector of the toggle element.
 * @param {boolean} expected - The expected state of the toggle
 */
export function setMaterialToggleState(selector: string, expected: boolean): void {
  const expectedVal = expected ? 'true' : 'false';
  verifySelectorVisibility(selector);
  cy.get(selector)
    .invoke('attr', 'aria-checked')
    .then((currentVal) => {
      if (currentVal !== expectedVal) {
        cy.get(selector).click();
      }
      cy.get(selector).invoke('attr', 'aria-checked').should('eq', expectedVal);
    });
}

/**
 * Sets the value of an autocomplete input field.
 *
 * Clears the input field, types the desired value, and selects the matching option from the dropdown.
 *
 * @param {string} selector - selector of the autocomplete input field.
 * @param {string} value - The value to be entered and selected.
 */
export function setAutocompleteValue(selector: string, value: string): void {
  verifySelectorVisibility(selector);
  cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_S }).clear();
  cy.get(selector).type(value);
  cy.contains('mat-option', value).should('be.visible');
  cy.contains('mat-option', value).first().click();
}

/**
 * Sets a value in a <input type="text"> datepicker by typing the date string.
 *
 * @param selector - ID selector of the input field (e.g. '#input-datepicker-to')
 * @param value - The date string to enter (e.g. '2025-12-31')
 */
export function setDatepickerValue(selector: string, value: string): void {
  verifySelectorVisibility(selector);
  cy.get(selector).clear();
  cy.get(selector).type(resolvePlaceholders(value));
  cy.get(selector).blur();
}

/**
 * Clicks a tab by index and verifies it's selected.
 *
 * @param index - Zero-based index of the tab to activate.
 */
export function switchToTab(index: number) {
  cy.get('[role="tab"]').eq(index).should('be.visible');
  cy.get('[role="tab"]').eq(index).click();
  cy.get('[role="tab"]').eq(index).should('have.attr', 'aria-selected', 'true');
}

/**
 * Sets the checkbox to the given checked state by clicking the label if needed.
 * @param selector - The base selector containing the checkbox input.
 * @param checked - Desired checked state (true or false).
 */
export function setCheckboxValue(selector: string, checked: boolean): void {
  verifySelectorVisibility(selector);
  cy.get(selector + ' input[type="checkbox"]')
    .should('exist')
    .then((checkbox) => {
      const isChecked = checkbox.prop('checked') as boolean;
      if (isChecked !== checked) {
        cy.get(selector + ' .mdc-label').click();
      }
    });
}

/**
 * Sets the value of a <app-wysiwyg> editor using only the ID selector.
 *
 * Locates the ProseMirror contenteditable div and types the new value.
 *
 * @param selector - The ID selector of the <app-wysiwyg> component.
 * @param value - The text to enter.
 */
export function setWysiwygValue(selector: string, value: string): void {
  const innerEditor = selector + " div.ProseMirror[contenteditable='true']";
  cy.get(innerEditor).click();
  cy.get(innerEditor).clear({ force: true });
  cy.get(innerEditor).type(value, { force: true });
}
