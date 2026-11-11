import {Then, When} from '@badeball/cypress-cucumber-preprocessor';
import {BaseWorkflow} from '../../models/base.workflow';
import {WorkflowFactory} from '../../models/workflow.factory';
import {getFeatureContextValue, setFeatureContextValue} from '../../helper/common/feature-context.helper';

When('I click {string} on {string}', (id: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickById(id);
});

When('I click {string} on {string} and wait for path {string}', (id: string, pageId: string, path: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickAndWaitForUrl(id, path);
});

When('I click {string} on {string} without loading wait', (id: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickById(id, undefined, false);
});

When('I click {string} on {string} in the front dialog', (selector: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickOnFrontDialog(selector);
});

When('I fill {string} with {string} on {string}', (field: string, value: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).fillForm({ [field]: value });
});

When('I store text of element {string} on {string} as {string}', (id: string, pageId: string, key: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).storeTextById(id, key);
});

When('I store value {string} as {string}', (key: string, value: string) => {
  setFeatureContextValue(key, value);
});

Then('I get the stored value {string}', (key: string) => {
  getFeatureContextValue(key);
});

Then('I expect {string} on {string} to be enabled', (id: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).assertEnabledById(id);
});

Then('I verify the deactivated button {string} on {string} to be enabled', (id: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).assertEnabledById(id, undefined, true);
});

When('I click on {string} if the button is present on {string}', (id: string, pageId: string) => {
  WorkflowFactory.get<BaseWorkflow>(pageId).clickIfIdIsPresent(id);
});

Then(
  'I click the button {string} within {string} to be {string} on {string}',
  (id: string, componentId: string, selection: string, pageId: string) => {
    WorkflowFactory.get<BaseWorkflow>(pageId).clickByComponentId(id, componentId, selection);
  },
);

Then(
  'I verify the button {string} within {string} to be {string} on {string}',
  (id: string, componentId: string, selection: string, pageId: string) => {
    WorkflowFactory.get<BaseWorkflow>(pageId).verifyByComponentId(id, componentId, selection);
  },
);
