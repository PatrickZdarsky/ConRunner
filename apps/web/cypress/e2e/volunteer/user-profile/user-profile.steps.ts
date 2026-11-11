import {WorkflowFactory} from '../../../models/workflow.factory';
import {UserProfileWorkflow} from './user-profile.workflow';
import {Given} from "@badeball/cypress-cucumber-preprocessor";


const wf = WorkflowFactory.get<UserProfileWorkflow>('userProfile');

Given('I navigate to the user profile page', () => {
  wf.visitUserSettingsPage();
});

Given('I verify the user profile page', () => {
   wf.verifyUserSettingsPage()
});

Given('The user profile page tiles exist', () => {
   wf.verifyAllTilesAreShown()
});

Given('I navigate to the user profile page from the header', () => {
   wf.navigateFromHeader();
});
