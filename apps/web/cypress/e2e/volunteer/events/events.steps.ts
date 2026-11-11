import {Given, Then} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {EventsWorkflow} from "./events.workflow";

const wf = WorkflowFactory.get<EventsWorkflow>("eventsList");

Given("I navigate to the events list page", () => {
  wf.visitEventsPage();
});

Then("I should see the event {string}", (eventName: string) => {
  cy.contains(".card", eventName).should("be.visible");
});
