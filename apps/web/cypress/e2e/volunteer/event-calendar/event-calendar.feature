Feature: Volunteer event calendar

  Background:
    Given I log in as volunteer
    And I open the event page

  Scenario: Open event calendar
    When I open the event calendar from the event page
    Then I should see the event calendar content
