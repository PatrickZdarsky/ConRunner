import {Given, Then, When} from "@badeball/cypress-cucumber-preprocessor";
import {APP_CONFIG} from "../../../config";
import SELECTORS_ADMIN_EVENT from "./event.selectors";

Given("I open the admin event page", () => {
  cy.visit(`${APP_CONFIG.BASE_URL}events`);
  cy.contains(".card", SELECTORS_ADMIN_EVENT.EVENT.name).click();
  cy.location("pathname").should("eq", `/events/${SELECTORS_ADMIN_EVENT.EVENT.id}`);
});

When("I open the manage event page from the event page", () => {
  cy.contains(".card", SELECTORS_ADMIN_EVENT.CARDS.MANAGE_EVENT).click();
  cy.location("pathname").should("eq", `/events/${SELECTORS_ADMIN_EVENT.EVENT.id}/manage`);
});

Then("I should see the admin event cards", () => {
  cy.contains(".card", SELECTORS_ADMIN_EVENT.CARDS.MANAGE_EVENT).should("be.visible");
  cy.contains(".card", SELECTORS_ADMIN_EVENT.CARDS.PLANNER_DASHBOARD).should("be.visible");
  cy.contains(".card", SELECTORS_ADMIN_EVENT.CARDS.EVENT_CALENDAR).should("be.visible");
  cy.contains(".card", "Volunteer Dashboard").should("not.exist");
});

Then("I should not see the volunteer dashboard card", () => {
  cy.contains(".card", "Volunteer Dashboard").should("not.exist");
});
