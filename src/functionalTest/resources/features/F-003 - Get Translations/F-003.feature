#====================================
@F-003
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
    And the response [has the 200 OK code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
