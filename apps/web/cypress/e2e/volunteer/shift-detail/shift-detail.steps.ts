import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {ShiftDetailWorkflow} from "./shift-detail.workflow";
import SELECTORS_SHIFT_DETAIL from "./shift-detail.selectors";

const wf = WorkflowFactory.get<ShiftDetailWorkflow>("shiftDetail");

When("I open the shift details for shift {string}", (shiftId: string) => {
  wf.visitShift(shiftId);
  cy.location("pathname").should("eq", `/shifts/${shiftId}`);
});

Then("I should see the shift details content", () => {
  cy.contains("h1", SELECTORS_SHIFT_DETAIL.PAGE.heading).should("be.visible");
  cy.get("app-manage-shift").should("exist");
});
