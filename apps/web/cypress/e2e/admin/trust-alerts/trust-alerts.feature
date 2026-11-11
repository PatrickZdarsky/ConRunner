Feature: Admin trust alerts

  Background:
    Given I log in as admin

  Scenario: Open trust alerts page
    When I open the admin page "trust"
    Then I should see the admin heading "Trust Alerts"
