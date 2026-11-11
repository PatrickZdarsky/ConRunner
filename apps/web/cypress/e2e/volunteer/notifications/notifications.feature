Feature: Volunteer notifications

  Background:
    Given I log in as volunteer

  Scenario: Open notifications page
    When I open notifications from the header
    Then I should see the empty notifications state
