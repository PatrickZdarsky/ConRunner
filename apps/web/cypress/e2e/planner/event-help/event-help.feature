Feature: Planner event help

  Background:
    Given I log in as volunteer
    And I open the planner event page

  Scenario: Open planner event help
    When I open the planner event help from the event page
    Then I should see the event help content
