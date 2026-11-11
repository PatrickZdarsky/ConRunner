Feature: Planner home

  Background:
    Given I log in as volunteer

  Scenario: Planner should not see admin cards
    When I navigate to the home page
    Then I should not see the admin home cards
