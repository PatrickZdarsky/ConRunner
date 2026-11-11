
import { BaseWorkflow } from '../../models/base.workflow';
import { DataTable } from '@badeball/cypress-cucumber-preprocessor';
import { resolvePlaceholders } from '../date/date-value-placeholder.helper';
import { APP_CONFIG } from '../../config';
import { clickElement } from '../form/form.helper';

/**
 * Clicks a button inside a table row by row selector and row index.
 * Supports either button index or a specific button selector.
 *
 * @param rowSelector - e.g. "#row-data-"
 * @param rowIndex - postfix of the row, e.g. "abc-123"
 * @param button - either button index (default: 0) or a selector like "#button-deprecate"
 */
export function clickButtonInTableRow(rowSelector: string, rowIndex: string, button: number | string = 0): void {
  const base = rowSelector + rowIndex;
  const index = parseInt(rowIndex, 10);
  if (typeof button === 'number') {
    const clickable = base + ' button, ' + base + ' a, ' + base + ' i';
    cy.get(clickable, { timeout: APP_CONFIG.TIMEOUT_M }).eq(button).as('target');
  } else {
    const scoped = base + ' ' + button;
    const combined = scoped + ', ' + button;
    cy.get(combined, { timeout: APP_CONFIG.TIMEOUT_M }).eq(index).as('target');
  }
  cy.get('@target').scrollIntoView();
  cy.get('@target').click();
}

/**
 * Verifies that the table row at given index contains or does not contain specific texts.
 *
 * @param rowPrefix - ID prefix (e.g. '#row-')
 * @param rowIndex - Zero-based index of the row
 * @param texts - Strings to check in the row
 * @param contains - If true, checks for presence; if false, checks for absence
 */
export function verifyTableRowContains(rowPrefix: string, rowIndex: number, texts: string[], contains: boolean): void {
  getTableRowPostfixes(rowPrefix).then((postfixes) => {
    const postfix = postfixes[rowIndex];
    const fullRowSelector = rowPrefix + postfix;
    cy.get(fullRowSelector, { timeout: APP_CONFIG.TIMEOUT_M }).scrollIntoView();
    cy.get(fullRowSelector).should('be.visible');
    texts.forEach((text) => {
      const assertion = contains ? 'contain' : 'not.contain';
      cy.get(fullRowSelector).should(assertion, text);
    });
  });
}

/**
 * Verifies that at least one table row contains all expected texts.
 *
 * @param rowPrefix - The row ID prefix (e.g., '#row-')
 * @param expectedTexts - List of texts that must all appear in the same row
 * @throws Error if no matching row is found
 */
export function verifyTableContainsTextInAnyRow(rowPrefix: string, expectedTexts: string[]): Cypress.Chainable<boolean> {
  let found = false;
  return getTableRowPostfixes(rowPrefix)
    .then((postfixes) => {
      postfixes.forEach((pf) => {
        const selector = rowPrefix + pf;
        cy.get(selector, { timeout: APP_CONFIG.TIMEOUT_M }).scrollIntoView();
        cy.get(selector)
          .invoke('text')
          .then((text) => {
            if (expectedTexts.every((t) => text.includes(t))) {
              found = true;
            }
          });
      });
    })
    .then(() => found);
}

/**
 * Assert that every table row under `rowPrefix` contains at least one of the given texts.
 * Throws an Error (failing the test) if any row contains none of them.
 * @param {string} rowPrefix - Selector prefix for table rows (e.g., '#row-').
 * @param {string[]} expectedTexts - List of texts; each row must include at least one.
 */
export function verifyTableEveryRowHasOne(rowPrefix: string, expectedTexts: string[]): void {
  getTableRowPostfixes(rowPrefix).then((pfs) => {
    pfs.forEach((pf) => {
      const s = rowPrefix + pf;
      cy.get(s, { timeout: APP_CONFIG.TIMEOUT_M })
        .invoke('text')
        .then((t) => {
          if (!expectedTexts.some((x) => t.includes(x))) throw new Error('missing expected texts in row: ' + s);
        });
    });
  });
}

/**
 * Returns the number of visible table rows matching a given ID prefix.
 *
 * @param rowPrefix - The ID prefix of the table rows (e.g. "#row-")
 * @returns A Cypress chainable containing the number of matched rows
 *
 */
export function getTableRowCount(rowPrefix: string): Cypress.Chainable<number> {
  const prefix = rowPrefix.startsWith('#') ? rowPrefix.slice(1) : rowPrefix;
  return cy.get('tbody tr[id]', { timeout: APP_CONFIG.TIMEOUT_M }).then((els) => {
    const count = Array.from(els).filter((el) => el.id.startsWith(prefix)).length;
    return cy.wrap(count);
  });
}

/**
 * Verifies that a table contains or does not contain the given data rows.
 * @param workflow - actual workflow
 * @param tableKey - The selector key to resolve the table.
 * @param dataTable - The data table with expected values from the feature file.
 * @param shouldContain - Whether the rows should exist (true) or not exist (false).
 */
export function verifyTableData(workflow: BaseWorkflow, tableKey: string, dataTable: DataTable, shouldContain: boolean): void {
  dataTable.raw().forEach((row, rowIndex) => {
    const resolvedRow = row.map((cell) => (typeof cell === 'string' ? resolvePlaceholders(cell) : cell));
    cy.log('Validating that table ' + (shouldContain ? 'contains' : 'does NOT contain') + ' texts of row ' + rowIndex + ':', resolvedRow);
    workflow.verifyTableContainsTextInAnyRow(tableKey, resolvedRow, shouldContain);
  });
}

/**
 * Verifies each row of a table by matching cell texts in order.
 * * ignoring the first row (assumed to be headers for readability).
 *
 * @param workflow - The workflow instance providing table selectors.
 * @param tableKey - The key identifying the table in the selector map.
 * @param dataTable - The expected table data, including header row.
 */
export function verifyTableDataInExactOrder(workflow: BaseWorkflow, tableKey: string, dataTable: DataTable): void {
  const rows = dataTable.raw().slice(1);
  rows.forEach((row, rowIndex) => {
    const resolvedRow = row.map((cell) => (typeof cell === 'string' ? resolvePlaceholders(cell) : cell));
    cy.log('Validating row ' + rowIndex + ' of table ' + tableKey + ':', resolvedRow);
    workflow.verifyTableRowTexts(tableKey, rowIndex, resolvedRow);
  });
}

/**
 * Finds the first table row that contains all expected texts and clicks a button inside that row.
 *
 * @param rowPrefix - ID prefix for the row selector (e.g. "#row-")
 * @param expectedTexts - All texts that must be present in the same row
 * @param button - Button index (number) or selector string (e.g. "#link-edit")
 */
export function clickButtonInMatchingRow(rowPrefix: string, expectedTexts: string[], button: number | string = 0): void {
  const cleanPrefix = rowPrefix.startsWith('#') ? rowPrefix.slice(1) : rowPrefix;
  cy.get('tbody tr[id]').then((rows) => {
    const row = Array.from(rows).find((el) => expectedTexts.every((t) => el.innerText.includes(t)));
    if (!row) {
      throw new Error('No matching row found for: ' + expectedTexts.join(', '));
    }
    const postfix = row.id.slice(cleanPrefix.length);
    if (typeof button === 'string' && button.endsWith('-')) {
      clickElement(button + postfix);
    } else {
      clickButtonInTableRow(rowPrefix, postfix, button);
    }
  });
}

/**
 * Returns the ID postfix of the table row at given index.
 *
 * @param rowPrefix - ID prefix (e.g. '#row-')
 * @returns Postfix string (e.g. '123' from '#row-123')
 */
export function getTableRowPostfixes(rowPrefix: string): Cypress.Chainable<string[]> {
  const cleanPrefix = rowPrefix.startsWith('#') ? rowPrefix.slice(1) : rowPrefix;
  return cy.get('tbody tr[id]', { timeout: APP_CONFIG.TIMEOUT_M }).then((rows) =>
    Array.from(rows)
      .map((el) => el.id)
      .filter((id) => id.startsWith(cleanPrefix))
      .map((id) => id.slice(cleanPrefix.length)),
  );
}
