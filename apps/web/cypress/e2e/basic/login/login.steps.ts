import {WorkflowFactory} from '../../../models/workflow.factory';
import {LoginWorkflow} from './login.workflow';
import {Given, When} from '@badeball/cypress-cucumber-preprocessor';
import {APP_CONFIG} from '../../../config';

const wf = WorkflowFactory.get<LoginWorkflow>('login');

Given('I log in as volunteer', () => {
  wf.visitLoginPage();
  wf.loginViaKeycloak(APP_CONFIG.USERNAME, APP_CONFIG.PASSWORD);
  // todo ad seperate admin and user username
});

Given('I login as user', () => {
  wf.visitLoginPage();
  wf.loginViaKeycloak(APP_CONFIG.USERNAME, APP_CONFIG.PASSWORD);
  // todo ad seperate admin and user username
});

Given('I log in as admin', () => {
  const adminUser = (Cypress.env("ADMIN_USERNAME") as string) || "testadmin";
  const adminPass = (Cypress.env("ADMIN_PASSWORD") as string) || "test";
  wf.visitLoginPage();
  wf.loginViaKeycloak(adminUser, adminPass);
});

When('I log out completely from FreeFinance', () => {
  wf.logout();
});
