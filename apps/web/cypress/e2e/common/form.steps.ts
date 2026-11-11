import {DataTable, Then, When} from '@badeball/cypress-cucumber-preprocessor';
import {switchToTab} from '../../helper/form/form-input.helper';
import {WorkflowFactory} from '../../models/workflow.factory';
import {BaseWorkflow} from '../../models/base.workflow';

const normalizeValue = (value: string): string | boolean => {
  if (value === 'true') return true;
  if (value === 'false') return false;
  return value;
};

Then('I verify {string} has value {string} on {string}', (field: string, expected: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).verifyFormData({ [field]: expected });
});

When('I enter the following data into the form on {string}', (pageId: string, dataTable: DataTable) => {
  const wf = WorkflowFactory.get<BaseWorkflow>(pageId);
  const [headers, ...rows] = dataTable.raw();
  rows.forEach((row) => {
    const data = Object.fromEntries(headers.map((key, i) => [key, normalizeValue(row[i])]));
    cy.log('Parsed Form Data:', JSON.stringify(data));
    wf.fillForm(data);
  });
});

Then('the form should contain the following data on {string}', (pageId: string, dataTable: DataTable) => {
  const wf = WorkflowFactory.get<BaseWorkflow>(pageId);
  const [headers, ...rows] = dataTable.raw();
  rows.forEach((row) => {
    const expectedData = Object.fromEntries(headers.map((key, index) => [key, normalizeValue(row[index])]));
    cy.log('Validating Form Data:', JSON.stringify(expectedData));
    wf.verifyFormData(expectedData);
  });
});

When('I submit the {string} form on {string}', (submitId: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).submitFormById(submitId, false);
});

When('I submit the {string} form on {string} and wait until it disappears', (submitId: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).submitFormById(submitId, true);
});

When('I submit the form by type submit on {string}', (pageId: string) => {
  return WorkflowFactory.get<BaseWorkflow>(pageId).submitFormByTypeSubmit(false);
});

When('I submit the form by type submit on {string} and wait until it disappears', (pageId: string) => {
  return WorkflowFactory.get<BaseWorkflow>(pageId).submitFormByTypeSubmit(true);
});

Then('I switch to tab number {int}', (tabIndex: number) => {
  switchToTab(tabIndex);
});
