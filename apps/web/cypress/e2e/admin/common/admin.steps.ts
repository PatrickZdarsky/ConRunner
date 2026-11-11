import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {APP_CONFIG} from "../../../config";

When("I open the admin page {string}", (path: string) => {
  cy.visit(`${APP_CONFIG.BASE_URL}${path}`);
  cy.location("pathname").should("eq", `/${path}`);
});

Then("I should see the admin heading {string}", (heading: string) => {
  cy.contains("h1", heading).should("be.visible");
});
