Feature: Planner shift plans

  Background:
    Given I log in as volunteer

  Scenario: Open planner dashboard
    When I open the planner dashboard page
    Then I should see the planner dashboard content
