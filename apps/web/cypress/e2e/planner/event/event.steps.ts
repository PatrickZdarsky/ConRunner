import {Given, Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {PlannerEventWorkflow} from "./event.workflow";
import SELECTORS_PLANNER_EVENT from "./event.selectors";
import {APP_CONFIG} from "../../../config";

const wf = WorkflowFactory.get<PlannerEventWorkflow>("plannerEvent");

Given("I open the planner event page", () => {
  cy.visit(`${APP_CONFIG.BASE_URL}events`);
  cy.contains(".card", SELECTORS_PLANNER_EVENT.EVENT.name).click();
  cy.location("pathname").should("eq", `/events/${SELECTORS_PLANNER_EVENT.EVENT.id}`);
});

When("I open the planner dashboard from the event page", () => {
  wf.openPlannerDashboard();
});

Then("I should see the planner event cards", () => {
  cy.contains(".card", SELECTORS_PLANNER_EVENT.CARDS.PLANNER_DASHBOARD).should("be.visible");
  cy.contains(".card", SELECTORS_PLANNER_EVENT.CARDS.EVENT_CALENDAR).should("be.visible");
  cy.contains(".card", SELECTORS_PLANNER_EVENT.CARDS.VOLUNTEER_DASHBOARD).should("be.visible");
  cy.contains(".card", SELECTORS_PLANNER_EVENT.CARDS.HELP).should("be.visible");
  cy.contains(".card", "Manage Event").should("not.exist");
});
