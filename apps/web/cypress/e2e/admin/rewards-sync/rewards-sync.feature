Feature: Admin rewards sync

  Background:
    Given I log in as admin

  Scenario: Open rewards sync page
    When I open the admin page "rewards-sync"
    Then I should see the admin heading "Share Reward Points"
