# Default OIDC Client Secret Configuration

## Finding

`src/main/resources/application.yaml` defined an OAuth2 client registration for `oidc` with both the client id and client secret set to `internal`.

The service uses Spring Security as a resource server for bearer-token JWT validation. No production code was found that uses the configured OAuth2 client registration to obtain outbound tokens, so the default client secret was unused application configuration rather than a required runtime secret.

## Risk

Leaving a hardcoded default client secret in main application configuration creates avoidable security noise and could become a credential risk if future code starts using that client registration in a non-test environment.

## Resolution

The unused Spring OAuth2 client registration and its hardcoded default secret have been removed. The service remains configured as an OAuth2 resource server for validating callers' inbound bearer JWTs, while the production IDAM user-info lookup, S2S token generation, and S2S token validation/authorization wiring are unchanged.

JWT resource-server validation remains configured through `spring-boot-starter-oauth2-resource-server` and the existing `JwtDecoder`.

## Verification

Check that the default secret and OAuth2 client wiring are absent outside this note and the guard test:

```bash
rg -n "client-secret: internal|spring-boot-starter-oauth2-client|\\.oauth2Client\\(|TestIdamConfiguration|ClientRegistrationRepository|OAuth2AuthorizedClient" --glob '!docs/**' --glob '!src/test/java/uk/gov/hmcts/reform/translate/config/SecurityConfigurationTest.java' .
```

Run the focused security regression test and the broader suites:

```bash
./gradlew test --tests uk.gov.hmcts.reform.translate.config.SecurityConfigurationTest
./gradlew test integration --continue
./gradlew checkstyleMain checkstyleTest checkstyleIntegrationTest
```
