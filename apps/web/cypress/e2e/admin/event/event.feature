Feature: Admin event page

  Background:
    Given I log in as admin

  Scenario: Open admin event page
    Given I open the admin event page
    Then I should see the admin event cards

  Scenario: Admin should not see volunteer dashboard
    Given I open the admin event page
    Then I should not see the volunteer dashboard card
