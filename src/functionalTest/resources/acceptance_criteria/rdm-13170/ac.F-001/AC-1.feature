#====================================
@RDM-13170.AC-001
Feature: AC-001: Invalid schema
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-000 @Smoke

  Scenario: Invalid schema (mandatory parent elements missing) - Return 400 error

    When the new end-point GET / has been successfully established
    And a call is submitted to the [GET /] operation of the [Translation Service] API
    And a user [with active profile to access the end-point exists]

    Then a positive response is received
    And the response [has the 200 - Ok status]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
