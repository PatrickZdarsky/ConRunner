import {Then} from "@badeball/cypress-cucumber-preprocessor";

Then("I should see the event calendar content", () => {
  cy.contains("h1", "Event Calendar").should("be.visible");
  cy.get("app-shift-calendar-grid").should("exist");
});
