import {DataTable, Then, When} from '@badeball/cypress-cucumber-preprocessor';

import {verifyTableData, verifyTableDataInExactOrder} from '../../helper/table/table.helper';
import {resolvePlaceholders} from '../../helper/date/date-value-placeholder.helper';
import {WorkflowFactory} from '../../models/workflow.factory';
import {BaseWorkflow} from '../../models/base.workflow';

Then(
  'the text in row {int} of {string} on {string} should contain',
  (rowIndex: number, tableKey: string, pageId: string, dataTable: DataTable) => {
    const resolved = dataTable.raw().flat().map(resolvePlaceholders);
    WorkflowFactory.get<BaseWorkflow>(pageId).verifyTableRowTexts(tableKey, rowIndex, resolved, true);
  },
);

Then(
  'the text in row {int} of {string} on {string} should not contain',
  (rowIndex: number, tableKey: string, pageId: string, dataTable: DataTable) => {
    const resolved = dataTable.raw().flat().map(resolvePlaceholders);
    WorkflowFactory.get<BaseWorkflow>(pageId).verifyTableRowTexts(tableKey, rowIndex, resolved, false);
  },
);

Then('every row of {string} on {string} should contain at least one of', (tableKey: string, pageId: string, dataTable: DataTable) => {
  const texts = dataTable.raw().flat().map(resolvePlaceholders);
  WorkflowFactory.get<BaseWorkflow>(pageId).verifyTableEveryRowHasOne(tableKey, texts);
});

Then('the table {string} on {string} should have {int} rows', (tableKey: string, pageId: string, expectedCount: number) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).verifyTableRowCount(tableKey, expectedCount);
});

Then('the table {string} on {string} should contain the following data', (tableKey: string, pageId: string, dataTable: DataTable) => {
  verifyTableData(WorkflowFactory.get<BaseWorkflow>(pageId), tableKey, dataTable, true);
});

Then('the table {string} on {string} should not contain the following data', (tableKey: string, pageId: string, dataTable: DataTable) => {
  verifyTableData(WorkflowFactory.get<BaseWorkflow>(pageId), tableKey, dataTable, false);
});

Then(
  'the table {string} on {string} should contain the following data in exact row order',
  (tableKey: string, pageId: string, dataTable: DataTable) => {
    verifyTableDataInExactOrder(WorkflowFactory.get<BaseWorkflow>(pageId), tableKey, dataTable);
  },
);

Then(
  'I click button {int} in the first row of {string} on {string} containing',
  (buttonIndex: number, tableKey: string, pageId: string, dataTable: DataTable) => {
    const expectedTexts = dataTable.raw().flat().map(resolvePlaceholders);
    WorkflowFactory.get<BaseWorkflow>(pageId).clickButtonInFirstMatchingRow(tableKey, expectedTexts, buttonIndex);
  },
);

Then(
  'I click button {string} in the first row of {string} on {string} containing',
  (buttonSelector: string, tableKey: string, pageId: string, dataTable: DataTable) => {
    const expectedTexts = dataTable.raw().flat().map(resolvePlaceholders);
    WorkflowFactory.get<BaseWorkflow>(pageId).clickButtonInFirstMatchingRow(tableKey, expectedTexts, buttonSelector);
  },
);

When('I click {string} in row {int} of {string} on {string}', (buttonSelector: string, row: number, rowKey: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickButtonInRow(rowKey, row, buttonSelector);
});
