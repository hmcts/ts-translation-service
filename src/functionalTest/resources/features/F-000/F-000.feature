#====================================
@F-000 @Smoke
Feature: F-000: Healthcheck Operation
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-000 @Smoke
  Scenario: must return a successful response from the Healthcheck Operation

    When a request is prepared with appropriate values
    And it is submitted to call the [Healthcheck] operation of [Translation Service]

    Then a positive response is received
    And the response [has the 200 OK code]
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
