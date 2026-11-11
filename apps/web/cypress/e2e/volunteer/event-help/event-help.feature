Feature: Volunteer event help

  Background:
    Given I log in as volunteer
    And I open the event page

  Scenario: Open event help
    When I open the event help from the event page
    Then I should see the event help content
