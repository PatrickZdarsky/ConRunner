import {Then} from "@badeball/cypress-cucumber-preprocessor";

Then("I should see the volunteer dashboard content", () => {
  cy.contains("h1", "Your Timeline").should("be.visible");
  cy.contains("h1", "Community Shift Offers").should("be.visible");
});
