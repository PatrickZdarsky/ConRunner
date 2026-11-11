Feature: Volunteer events list

  Background:
    Given I log in as volunteer

  Scenario: See events list
    When I navigate to the events list page
    Then I should see the event "Tech Innovators Summit 2025"
