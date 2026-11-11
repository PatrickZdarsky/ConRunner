import {Then} from "@badeball/cypress-cucumber-preprocessor";

Then("I should see the admin home cards", () => {
  cy.contains(".card h1", "Reward Points Sync").should("exist");
  cy.contains(".card h1", "Trust Alerts").should("exist");
  cy.contains(".card h1", "App Users").should("exist");
  cy.contains(".card h1", "Pretalx Sync").should("exist");
  cy.contains(".card h1", "Audit Log").should("exist");
});
