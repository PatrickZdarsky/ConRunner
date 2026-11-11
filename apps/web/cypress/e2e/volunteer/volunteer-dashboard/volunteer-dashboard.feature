Feature: Volunteer dashboard

  Background:
    Given I log in as volunteer
    And I open the event page

  Scenario: Open volunteer dashboard
    When I open the volunteer dashboard from the event page
    Then I should see the volunteer dashboard content
