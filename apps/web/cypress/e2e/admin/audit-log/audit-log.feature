Feature: Admin audit log

  Background:
    Given I log in as admin

  Scenario: Open audit log page
    When I open the admin page "audit"
    Then I should see the admin heading "Audit Log"

  Scenario: Filter to empty audit log
    When I open the admin page "audit"
    And I filter audit log with "no-events-type" and "no-events-key"
    Then I should see no audit log events
