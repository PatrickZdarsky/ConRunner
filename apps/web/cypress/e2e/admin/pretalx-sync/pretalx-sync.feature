Feature: Admin pretalx sync

  Background:
    Given I log in as admin

  Scenario: Open pretalx sync page
    When I open the admin page "pretalx-sync"
    Then I should see the admin heading "Synchronize Events with Pretalx"
