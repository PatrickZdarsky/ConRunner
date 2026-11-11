Feature: Shift page

  Background:
    Given I log in as volunteer

  Scenario: Open shift details
    When I open the shift details for shift "1"
    Then I should see the shift details content
