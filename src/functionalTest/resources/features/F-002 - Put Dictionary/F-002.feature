#====================================
@F-002
Feature: F-002: Put Dictionary Operation
#====================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.1 #WLTS-4 AC1
  Scenario: Add a new English to Welsh translation entry - Return 201 Success

    Given a user [with manage-translation IDAM role],
    When  a request is prepared with appropriate values
    And   the request [add a new English to Welsh translation]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 201 OK status code]
    And   the response has all other details as expected.
    And   a successful call [to verify translations] as in [F-002_Verify],


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.2 #WLTS-4 AC2
  Scenario: Update Welsh translation for existing English phrase - Return 201 Success

    Given a user [with manage-translation IDAM role],
    And   a successful call [to PUT translation phrases into the dictionary] as in [S-002.2_Existing_Data],
    When  a request is prepared with appropriate values
    And   the request [update Welsh phrase of an existing English phrase]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 201 OK status code]
    And   the response has all other details as expected.
    And   a successful call [to verify translations] as in [F-002_Verify],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.3 #WLTS-4 AC3
  Scenario: Add only English phrase - Return 201 Success

    Given a user [with load-translation IDAM role],
    When  a request is prepared with appropriate values
    And   the request [add a new English only phrase]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 201 OK status code]
    And   the response has all other details as expected.
    And   a successful call [to verify translations] as in [F-002_Verify],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.4 #WLTS-4 AC4
  Scenario: Forbidden for missing roles - Return 403 error

    Given a user [without manage-translation or load-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   the request [is a valid request with the wrong roles]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 403 Forbidden status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.5 #WLTS-4 AC5 - Translation is empty
  Scenario: Bad Request - Return 400 error when Translations are empty

    Given a user [with manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   the request [where the translations are empty]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.6 #WLTS-4 AC5 - Translation is null
  Scenario: Bad Request - Return 400 error when Translations are null

    Given a user [with manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   the request [where the translations are null]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-002.7 #WLTS 4 AC6
  Scenario: Welsh not allowed for this user - Return 400 error

    Given a user [without manage-translation IDAM role],
    When  a request is prepared with appropriate values,
    And   the request [has welsh phrases]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a negative response is received
    And   the response [has the 400 Bad Request status code]
    And   the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-002.8 #WLTS-20 AC1
  Scenario: Do nothing and return success - when service is definition store

    Given a user [without manage-translation or load-translation IDAM role and uses ccd-definition],
    And   a successful call [to PUT translation phrases into the dictionary] as in [S-002.8_Existing_Data],
    When  a request is prepared with appropriate values
    And   the request [contains an entry for an english phrase that already exists]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 201 OK status code]
    And   the response has all other details as expected.
    And   a successful call [to verify translations] as in [F-002_Verify],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-002.9 #WLTS-20 AC2
  Scenario: Add only English phrase and Return 201 Success - when service is definition store

    Given a user [with load-translation IDAM role with ccd-definition],
    When  a request is prepared with appropriate values
    And   the request [add a new English phrase]
    And   it is submitted to call the [PUT dictionary] operation of [Translation Service]
    Then  a positive response is received
    And   the response [has the 201 OK status code]
    And   the response has all other details as expected.
    And   a successful call [to verify translations] as in [F-002_Verify],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
