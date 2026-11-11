import {Then, When} from "@badeball/cypress-cucumber-preprocessor";

When("I filter application users with {string}", (query: string) => {
  cy.get('input[placeholder="Filter Users"]').clear().type(query);
});

Then("I should see no application users", () => {
  cy.contains("span", "No application users at this time.").should("be.visible");
});
