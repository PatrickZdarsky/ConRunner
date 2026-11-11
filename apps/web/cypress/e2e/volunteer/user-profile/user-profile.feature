Feature: User profile page

  Background:
    Given I log in as volunteer
    When I navigate to the user profile page
    Then I verify the user profile page

  Scenario: Press cards for routing to user profile page
    Given I navigate to the dashboard page
    Then I navigate to the user profile page from the header

  Scenario: The expected Tiles are visible
    Then The user profile page tiles exist

  Scenario: Profile tiles are visible
    And the form should contain the following data on "userProfile"
      | name | surname |
      | Kerbert  | Huttelwascher  |
    And the form should contain the following data on "userProfile"
      | autoAssigned | tradeRequested | tradeStatusChanged | shiftReminder |
      | Email        | None           | None               | None          |
