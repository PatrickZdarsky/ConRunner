Feature: Admin home

  Background:
    Given I log in as admin

  Scenario: See admin home cards
    When I navigate to the home page
    Then I should see the admin home cards
