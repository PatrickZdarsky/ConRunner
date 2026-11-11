Feature: Admin volunteer detail

  Background:
    Given I log in as admin

  Scenario: Open volunteer detail page
    When I open the volunteer detail page
    Then I should see the volunteer detail content
