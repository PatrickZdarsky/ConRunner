Feature: Planner event calendar

  Background:
    Given I log in as volunteer
    And I open the planner event page

  Scenario: Open planner event calendar
    When I open the planner event calendar from the event page
    Then I should see the event calendar content
