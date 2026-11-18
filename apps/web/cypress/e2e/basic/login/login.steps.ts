import {WorkflowFactory} from '../../../models/workflow.factory';
import {LoginWorkflow} from './login.workflow';
import {Given, When} from '@badeball/cypress-cucumber-preprocessor';

const wf = WorkflowFactory.get<LoginWorkflow>('login');

function loginAsUser(): void {
  cy.env(['USERNAME', 'PASSWORD']).then(({USERNAME, PASSWORD}) => {
    wf.visitLoginPage();
    wf.loginViaKeycloak(String(USERNAME ?? ''), String(PASSWORD ?? ''));
  });
}

Given('I log in as volunteer', loginAsUser);
Given('I login as user', loginAsUser);

Given('I log in as admin', () => {
  cy.env(['ADMIN_USERNAME', 'ADMIN_PASSWORD']).then(({ADMIN_USERNAME, ADMIN_PASSWORD}) => {
    wf.visitLoginPage();
    wf.loginViaKeycloak(String(ADMIN_USERNAME || 'testadmin'), String(ADMIN_PASSWORD || 'test'));
  });
});

When('I log out completely from FreeFinance', () => {
  wf.logout();
});
