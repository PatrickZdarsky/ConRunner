import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {APP_CONFIG} from "../../../config";

When("I open the planner shift details for shift {string}", (shiftId: string) => {
  cy.visit(`${APP_CONFIG.BASE_URL}shifts/${shiftId}`);
  cy.location("pathname").should("eq", `/shifts/${shiftId}`);
});

Then("I should see the planner shift edit controls", () => {
  cy.contains("h1", "Shift Details").should("be.visible");
  cy.get('input[value="Edit"]').should("exist");
});
