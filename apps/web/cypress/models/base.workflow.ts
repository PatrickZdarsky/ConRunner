
import {
  assertEnabledVisible,
  clickAndWaitForPath,
  clickElement,
  clickElementByComponent,
  clickFrontDialog,
  handleElementInput,
  verifyElementValue,
} from '../helper/form/form.helper';
import {
  clickButtonInMatchingRow,
  clickButtonInTableRow,
  getTableRowCount,
  getTableRowPostfixes,
  verifyTableContainsTextInAnyRow,
  verifyTableEveryRowHasOne,
  verifyTableRowContains,
} from '../helper/table/table.helper';
import { resolvePlaceholders } from '../helper/date/date-value-placeholder.helper';
import { setFeatureContextValue } from '../helper/common/feature-context.helper';
import {APP_CONFIG} from '../config';

type FlatSelectors = Record<string, string>;
type SelectorGroup = Record<string, string | FlatSelectors>;
type FormData = Record<string, string | boolean>;

export abstract class BaseWorkflow {
  protected selectors: SelectorGroup;
  private activeGroupName: string | null = null;

  protected constructor(selectors: SelectorGroup) {
    this.selectors = selectors;
  }

  submitFormById(submitId: string, waitUntilDisappears: boolean): void {
    const selector = this.resolveSelector(submitId);
    if (!selector) throw new Error('Submit selector not found for: ' + submitId);
    cy.get(selector).scrollIntoView();
    cy.get(selector).should('be.visible');
    cy.get(selector).should('be.enabled');
    cy.get(selector).click();
    if (waitUntilDisappears) {
      cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_L }).should('not.exist');
    }
  }

  submitFormByTypeSubmit(waitUntilDisappears: boolean): void {
    const s = 'button[type="submit"]:enabled, input[type="submit"]:enabled';
    cy.get(s).filter(':visible').first().click();
    if (waitUntilDisappears) cy.get('.cdk-overlay-container').find(s).should('not.exist');
  }

  fillForm(data: FormData): void {
    cy.log('Filling form...');
    for (const [field, value] of Object.entries(data)) {
      const selector = this.resolveSelector(field);
      if (selector) {
        const resolved = typeof value === 'string' ? resolvePlaceholders(value) : value;
        handleElementInput(selector, resolved);
      }
    }
  }

  verifyFormData(expectedData: FormData): void {
    cy.log('Verifying form data...');
    for (const [field, expectedValue] of Object.entries(expectedData)) {
      const selector = this.resolveSelector(field);
      if (selector) {
        const resolved = typeof expectedValue === 'string' ? resolvePlaceholders(expectedValue) : expectedValue;
        verifyElementValue(selector, resolved);
      }
    }
  }

  clickById(id: string, rowIndex?: number, waitForLoading: boolean = true) {
    const selector = this.resolveSelector(id);
    if (!selector) {
      throw new Error('Cannot click. No selector found for id: ' + id);
    }
    const finalSelector = typeof rowIndex !== 'undefined' && rowIndex !== null ? selector + rowIndex : selector;
    clickElement(finalSelector, waitForLoading);
  }

  clickAndWaitForUrl(id: string, expectedPathPart: string): void {
    const selector = this.resolveSelector(id);
    if (!selector) {
      throw new Error('Cannot click. No selector found for id: ' + selector);
    }
    clickAndWaitForPath(selector, expectedPathPart);
  }

  clickOnFrontDialog(id: string) {
    const selector = this.resolveSelector(id);
    if (!selector) {
      throw new Error('Cannot click. No selector found for id: ' + id);
    }
    clickFrontDialog(selector);
  }

  clickIfIdIsPresent(id: string): void {
    const selector = this.resolveSelector(id);
    if (!selector) return;
    cy.get('body')
      .find(selector, { timeout: APP_CONFIG.TIMEOUT_M })
      .filter(':visible:enabled')
      .its('length')
      .then((len) => {
        if (!len) return;
        cy.get(selector).filter(':visible:enabled').first().scrollIntoView();
        cy.get(selector).filter(':visible:enabled').first().click();
      });
  }

  verifyTableRowTexts(id: string, rowIndex: number, texts: string[], contains = true): void {
    const rowSelector = this.resolveSelector(id);
    if (!rowSelector) throw new Error('Row selector not defined.');
    verifyTableRowContains(rowSelector, rowIndex, texts, contains);
  }

  verifyTableContainsTextInAnyRow(id: string, texts: string[], shouldContain = true): void {
    let rowSelector: string;
    try {
      rowSelector = this.resolveSelector(id);
    } catch (e) {
      if (shouldContain) throw e;
      cy.log('Selector for ' + id + ' not present, assuming empty as expected.');
      return;
    }
    if (!rowSelector) throw new Error('Row selector not defined.');

    verifyTableContainsTextInAnyRow(rowSelector, texts).should('eq', shouldContain);
  }

  verifyTableEveryRowHasOne(id: string, texts: string[]): void {
    const rowSelector = this.resolveSelector(id);
    if (!rowSelector) throw new Error('Row selector not defined.');
    verifyTableEveryRowHasOne(rowSelector, texts);
  }

  clickButtonInRow(id: string, rowIndex: number, buttonSelector: string): void {
    const rowPrefix = this.resolveSelector(id);
    if (!rowPrefix) throw new Error('Row selector not defined.');
    getTableRowPostfixes(rowPrefix).then((list) => {
      const pf = list[rowIndex];
      if (pf == null) throw new Error('Row index out of range: ' + rowIndex);
      const btn = this.resolveSelector(buttonSelector) ?? buttonSelector;
      const btnFinal = btn.endsWith('-') ? btn + pf : btn;
      clickButtonInTableRow(rowPrefix, pf, btnFinal);
    });
  }

  verifyTableRowCount(id: string, rowsNumber: number): void {
    const rowSelectorPrefix = this.resolveSelector(id);
    if (rowsNumber != 0) {
      if (!rowSelectorPrefix) throw new Error('Row selector not defined.');
      getTableRowCount(rowSelectorPrefix).should('eq', rowsNumber);
    } else {
      cy.get('[id^="' + rowSelectorPrefix.replace('#', '') + '"]').should('not.exist');
    }
  }

  storeTextById(id: string, key: string): void {
    const selector = this.resolveSelector(String(id));
    if (!selector) {
      throw new Error('Cannot store text. No selector found for id: ' + id);
    }
    cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M })
      .should('be.visible')
      .invoke('text')
      .should('not.be.empty')
      .then((text) => {
        const value = String(text).trim();
        setFeatureContextValue(String(key), value);
        cy.log('Stored value from ' + selector + ' as ' + key + ': ' + value);
      });
  }

  clickButtonInFirstMatchingRow(id: string, texts: string[], button: number | string = 0): void {
    const rowSelector = this.resolveSelector(id);
    if (!rowSelector) {
      throw new Error('Row selector not defined for id: ' + id);
    }
    const resolvedTexts = texts.map((text) => (typeof text === 'string' ? resolvePlaceholders(text) : text));
    const resolvedButton = typeof button === 'string' ? (this.resolveSelector(button) ?? button) : button;
    clickButtonInMatchingRow(rowSelector, resolvedTexts, resolvedButton);
  }

  assertEnabledById(id: string, rowIndex?: number, deactivated?: boolean): void {
    const s = this.resolveSelector(id);
    if (!s) throw new Error('Selector not defined for id: ' + id);
    const fs = rowIndex != null ? s + rowIndex : s;
    assertEnabledVisible(fs, deactivated);
  }

  protected resolveSelectorGroupName(field: string): string | null {
    for (const [key, value] of Object.entries(this.selectors)) {
      if (typeof value !== 'string' && field in value) {
        return key;
      }
    }
    return null;
  }

  protected resolveSelector(field: string): string {
    const groupName = this.resolveSelectorGroupName(field);
    const group = groupName ? (this.selectors[groupName] as FlatSelectors) : (this.selectors as FlatSelectors);
    if (!(field in group)) {
      throw new Error(`No selector found for field: ${field}`);
    }
    this.activeGroupName = groupName;
    return group[field];
  }

  clickByComponentId(id: string, componentId: string, selection: string) {
    const selector = this.resolveSelector(id);
    const componentSelector = this.resolveSelector(componentId);
    if (!selector || !componentSelector) {
      throw new Error('Cannot click. No selector found for id: ' + id);
    }
    clickElementByComponent(selector, componentSelector, selection);
  }

  verifyByComponentId(id: string, componentId: string, expected: string) {
    const selector = this.resolveSelector(id);
    const componentSelector = this.resolveSelector(componentId);
    cy.get(componentSelector).within(() => {
      cy.get(selector).invoke('attr', 'aria-checked').should('eq', String(expected));
    });
  }
}
