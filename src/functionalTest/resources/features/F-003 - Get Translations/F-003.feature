#====================================
@F-003
@Ignore # To be done under WLTS-16
Feature: F-003: Get Translations
#====================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
#    And a successful call [to set up] as in [F-003_Translations_Data],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.1 #AC01
  Scenario: must return a successful response when translate operation is requested
    Given a user with [an active solicitor profile],
    When a request is prepared with appropriate values
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response [has 200 OK code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.4 #AC04.1
  Scenario: Invalid request - Return 400 error
    Given a user with [an active solicitor profile],
    When a request is prepared with appropriate values
    And the request [contains the mandatory elements but the format is not as expected]
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a negative response is received
    And the response [has 400 Bad Request code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.5 #AC04.2
  Scenario: Invalid request - Return 400 error
    Given a user with [an active solicitor profile],
    When a request is prepared with appropriate values
    And the request [contains an empty body]
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a negative response is received
    And the response [has 400 Bad Request code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
