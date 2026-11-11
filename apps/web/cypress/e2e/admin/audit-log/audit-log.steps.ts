import {Then, When} from "@badeball/cypress-cucumber-preprocessor";

When("I filter audit log with {string} and {string}", (typeValue: string, keyValue: string) => {
  cy.contains("h1", "Audit Log").should("be.visible");
  cy.contains("small", "Type").parent().find("xsb-input-text").find("input").clear().type(typeValue);
  cy.contains("small", "Key").parent().find("xsb-input-text").find("input").clear().type(keyValue);
});

Then("I should see no audit log events", () => {
  cy.contains("span", "No events at this time.").should("be.visible");
});
