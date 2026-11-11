import {Then, When} from "@badeball/cypress-cucumber-preprocessor";

When("I open notifications from the header", () => {
  cy.get(".header .notifications").should("be.visible").click();
  cy.location("pathname").should("eq", "/notifications");
});

Then("I should see the empty notifications state", () => {
  cy.contains("h1", "It's quiet for now").should("be.visible");
  cy.contains("span", "No notifications available.").should("be.visible");
});
