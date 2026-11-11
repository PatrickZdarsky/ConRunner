import {Then} from "@badeball/cypress-cucumber-preprocessor";

Then("I should see the manage event content", () => {
  cy.contains("h1", "Manage Event").should("be.visible");
  cy.get("xsb-input-multitoggle").should("exist");
});
