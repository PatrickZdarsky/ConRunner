import {WorkflowFactory} from "../../../models/workflow.factory";
import {Given} from "@badeball/cypress-cucumber-preprocessor";
import {ShiftDashBoardWorkflow} from "./shift-dashboard.workflow";

const wf = WorkflowFactory.get<ShiftDashBoardWorkflow>('shiftDashboard');

Given('I navigate to the dashboard page', () => {
  wf.visitShiftDashboardPage();
});

Given('I navigate to the user settings page from the dashboard page', () => {
  wf.visitUserSettingsPage();
});

Given('I navigate to the events page from the dashboard page', () => {
  wf.visitEventsPage();
});

Given('I navigate to a plan page from the dashboard page', () => {
  wf.visitPlanPage();
});

Given('I navigate to a shift page from the dashboard page', () => {
  wf.visitShiftPage();
});
