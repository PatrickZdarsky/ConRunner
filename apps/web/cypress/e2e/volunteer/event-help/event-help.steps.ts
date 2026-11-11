import {Then} from "@badeball/cypress-cucumber-preprocessor";

Then("I should see the event help content", () => {
  cy.contains("h1", "Help & Contacts").should("be.visible");
});
