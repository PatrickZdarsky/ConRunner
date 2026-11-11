Feature: Plan page

  Background:
    Given I log in as volunteer

  Scenario: Open shift plan invite
    When I open the shift plan invite "testinviterandomstring"
    Then I should see the shift plan invite content
