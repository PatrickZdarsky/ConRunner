Feature: Event page

  Background:
    Given I log in as volunteer

  Scenario: Open volunteer event page
    Given I open the event page
    Then I should see the volunteer event cards
