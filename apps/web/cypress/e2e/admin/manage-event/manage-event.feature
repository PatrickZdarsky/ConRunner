Feature: Admin manage event

  Background:
    Given I log in as admin
    And I open the admin event page

  Scenario: Open manage event page
    When I open the manage event page from the event page
    Then I should see the manage event content
