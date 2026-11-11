Feature: Admin application users

  Background:
    Given I log in as admin

  Scenario: Open application users page
    When I open the admin page "volunteers"
    Then I should see the admin heading "About Volunteers"

  Scenario: Filter to empty application users
    When I open the admin page "volunteers"
    And I filter application users with "no-matching-user"
    Then I should see no application users
