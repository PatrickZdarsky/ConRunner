import {Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {APP_CONFIG} from "../../../config";

const VOLUNTEER_ID = "28c02050-4f90-4f3a-b1df-3c7d27a166e5";

When("I open the volunteer detail page", () => {
  cy.visit(`${APP_CONFIG.BASE_URL}volunteers/${VOLUNTEER_ID}`);
  cy.location("pathname").should("eq", `/volunteers/${VOLUNTEER_ID}`);
});

Then("I should see the volunteer detail content", () => {
  cy.contains("h1", "Volunteer Details").should("be.visible");
  cy.contains("h1", "Volunteer Shift Plans").should("be.visible");
});
