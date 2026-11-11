import {When} from "@badeball/cypress-cucumber-preprocessor";
import SELECTORS_PLANNER_EVENT from "../event/event.selectors";

When("I open the planner event calendar from the event page", () => {
  cy.contains(".card", SELECTORS_PLANNER_EVENT.CARDS.EVENT_CALENDAR).click();
  cy.location("pathname").should("eq", `/events/${SELECTORS_PLANNER_EVENT.EVENT.id}/calendar`);
});
