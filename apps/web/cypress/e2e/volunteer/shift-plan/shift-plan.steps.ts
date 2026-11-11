import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {WorkflowFactory} from "../../../models/workflow.factory";
import {ShiftPlanWorkflow} from "./shift-plan.workflow";
import SELECTORS_SHIFT_PLAN from "./shift-plan.selectors";

const wf = WorkflowFactory.get<ShiftPlanWorkflow>("shiftPlans");

When("I open the shift plan invite {string}", (code: string) => {
  wf.visitInvite(code);
  cy.location("pathname").should("eq", `/onboarding/${code}`);
});

Then("I should see the shift plan invite content", () => {
  cy.contains("h1", SELECTORS_SHIFT_PLAN.INVITE.heading).should("be.visible");
});
