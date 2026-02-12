#====================================
@F-001
Feature: F-001: Get Dictionary Operation
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-001.1
  Scenario: Success response - Return 200 success containing all dictionary entries

    Given a user [with manage-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-001_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [GET dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.
    #
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-001.2
  Scenario: Must return a negative response when invoking user does not have manage-translations IDAM role

    Given a user [without manage-translation IDAM role],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [GET dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 403 Forbidden status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-001.3
  Scenario: Success response - Return 200 success containing dictionary entries (limited)

    Given a user [with manage-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-001_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [GET dictionary limited] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.
    #
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
