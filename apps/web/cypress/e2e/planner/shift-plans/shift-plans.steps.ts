import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {PlannerShiftPlansWorkflow} from "./shift-plans.workflow";
import SELECTORS_PLANNER_SHIFT_PLANS from "./shift-plans.selectors";
import SELECTORS_PLANNER_EVENT from "../event/event.selectors";

const wf = WorkflowFactory.get<PlannerShiftPlansWorkflow>("plannerShiftPlans");

When("I open the planner dashboard page", () => {
  wf.visitPlannerDashboard(SELECTORS_PLANNER_EVENT.EVENT.id);
  cy.location("pathname").should("eq", `/events/${SELECTORS_PLANNER_EVENT.EVENT.id}/plans`);
});

Then("I should see the planner dashboard content", () => {
  cy.contains("h1", SELECTORS_PLANNER_SHIFT_PLANS.PAGE.heading)
    .should("be.visible")
    .closest(".card")
    .within(() => {
      cy.get("xsb-input-select").should("exist");
      cy.get("xsb-input-multitoggle").should("exist");
    });
});
