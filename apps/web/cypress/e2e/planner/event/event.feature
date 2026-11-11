Feature: Planner event page

  Background:
    Given I log in as volunteer

  Scenario: Open planner event page
    Given I open the planner event page
    Then I should see the planner event cards
