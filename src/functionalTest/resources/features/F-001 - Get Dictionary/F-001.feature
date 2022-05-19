#====================================
@F-001
Feature: F-001: Get Dictionary Operation
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-001.2
  Scenario: must return a negative response when invoking user does not have manage-translations IDAM role

    Given a user [without manage-translation IDAM role],
    When a request is prepared with appropriate values
    And it is submitted to call the [GET dictionary] operation of [Translation Service]

    Then a negative response is received
    And the response [has the 403 Forbidden status code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
