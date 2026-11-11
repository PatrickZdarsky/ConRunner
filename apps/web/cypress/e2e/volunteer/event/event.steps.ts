import {Given, Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {EventWorkflow} from "./event.workflow";
import SELECTORS_EVENTS from "./event.selectors";
import {APP_CONFIG} from "../../../config";

const wf = WorkflowFactory.get<EventWorkflow>("events");

Given("I open the event page", () => {
  cy.visit(`${APP_CONFIG.BASE_URL}events`);
  cy.contains(".card", SELECTORS_EVENTS.EVENT.name).click();
  cy.location("pathname").should("eq", `/events/${SELECTORS_EVENTS.EVENT.id}`);
});

When("I open the volunteer dashboard from the event page", () => {
  wf.openVolunteerDashboard();
});

When("I open the event calendar from the event page", () => {
  wf.openEventCalendar();
});

When("I open the event help from the event page", () => {
  wf.openEventHelp();
});

Then("I should see the volunteer event cards", () => {
  cy.contains(".card", SELECTORS_EVENTS.CARDS.VOLUNTEER_DASHBOARD).should("be.visible");
  cy.contains(".card", SELECTORS_EVENTS.CARDS.EVENT_CALENDAR).should("be.visible");
  cy.contains(".card", SELECTORS_EVENTS.CARDS.HELP).should("be.visible");
  cy.contains(".card", "Manage Event").should("not.exist");
  cy.contains(".card", "Planner Dashboard").should("not.exist");
  cy.contains("h1", "About the Event").should("be.visible");
});
