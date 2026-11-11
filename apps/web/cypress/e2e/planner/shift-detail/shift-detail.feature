Feature: Planner shift details

  Background:
    Given I log in as volunteer

  Scenario: Open planner shift details
    When I open the planner shift details for shift "11"
    Then I should see the planner shift edit controls
