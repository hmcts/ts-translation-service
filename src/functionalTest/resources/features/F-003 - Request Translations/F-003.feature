#====================================
@F-003
Feature: F-003: Request Translations
#====================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a successful call [to PUT phrase with translation into the dictionary] as in [F-003_Put_Dictionary]
    And another successful call [to GET the dictionary] as in [F-003_Get_Dictionary]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.1 #WLTS-29 AC01
  Scenario: must return a successful response when translate operation is requested with no AUTHORIZATION header
    When a request is prepared with appropriate values
    And the request [is missing IDAM AUTHORIZATION token header]
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.2 #WLTS-3 AC02
  Scenario: must return a successful response when English phrase exists in dictionary with translation
    When a request is prepared with appropriate values
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.3 #WLTS-3 AC03
  Scenario: must return a successful response when English phrase exists in dictionary without translation
    When a request is prepared with appropriate values
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.4 #WLTS-3 AC04
  Scenario: Invalid request - Return 400 error
    When a request is prepared with appropriate values
    And the request [contains the mandatory elements but the format is not as expected]
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a negative response is received
    And the response [has 400 Bad Request code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.5 #WLTS-3 AC05
  Scenario: must return a successful response when duplicate English phrases exists in translation request
    When a request is prepared with appropriate values
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-003.6 #WLTS-3 AC01 / WLTS-29 AC05
  Scenario: must return a successful response when translate operation is requested
    Given a user with [an active solicitor profile]
    When a request is prepared with appropriate values
    And the request [has a valid IDAM AUTHORIZATION token header]
    And it is submitted to call the [translate] operation of [Translation Service]
    Then a positive response is received
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
