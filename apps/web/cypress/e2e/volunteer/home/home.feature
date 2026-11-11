Feature: Volunteer home

  Background:
    Given I log in as volunteer

  Scenario: See home content
    When I navigate to the home page
    Then I should see the home page content

  Scenario: Volunteer should not see admin cards
    When I navigate to the home page
    Then I should not see the admin home cards
