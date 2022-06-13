#====================================
@F-004
Feature: F-004: Put Dictionary Operation
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.1
  Scenario: Add a new English to Welsh translation entry - Return 201 Success

    Given a user [with manage-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-004_1_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [translate] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.2
  Scenario: Update Welsh translation for existing English phrase - Return 201 Success

    Given a user [with manage-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary 1] as in [F-004_2_1_Put_Dictionary_Entries],
    And   a successful call [to PUT translation phrases into the dictionary 2] as in [F-004_2_2_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [GET dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.3
  Scenario: Add only English phrase - Return 201 Success

    Given a user [with load-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-004_3_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [GET dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.4
  Scenario: Forbidden for missing roles - Return 403 error

    Given a user [without manage-translation or load-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 403 Forbidden status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.5
  Scenario: Bad Request - Return 400 error when Translations are empty

    Given a user [with manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.6 #AC5 - Translation is null
  Scenario: Bad Request - Return 400 error when Translations are null

    Given a user [with manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-004.7
  Scenario: Welsh not allowed for this user - Return 400 error

    Given a user [without manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-004.8
  Scenario: Do nothing and return success - when service is definition store

    Given a user [without manage-translation or load-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-004_8_Put_Dictionary_Entries],
    And   a successful call [to PUT translation phrases into the dictionary] as in [F-004_8_Put_Dictionary_Entries],
    When  a request is prepared with appropriate values
    And   it is submitted to call the [GET dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 200 OK status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-004.9
  Scenario: Add only English phrase and Return 201 Success - when service is definition store

     Given a user [with manage-translation IDAM role],
     And   a successful call [to PUT translation phrases into the dictionary] as in [F-004_9_Put_Dictionary_Entries],
     When  a request is prepared with appropriate values,
     And   it is submitted to call the [GET dictionary] operation of [Translation Service]
     Then  a positive response is received
     And   the response [has the 200 OK status code]
     And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
