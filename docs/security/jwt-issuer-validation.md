# JWT issuer validation

## Service

`ts-translation-service`

## Summary

- JWT issuer validation is enabled in the active decoder.
- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup.
- `oidc.issuer` is the default enforced issuer value and is sourced from `OIDC_ISSUER`.
- `oidc.allowed-issuers` is optional and is sourced from `OIDC_ALLOWED_ISSUERS`; when it is set, those issuers are accepted in addition to `OIDC_ISSUER`.
- See [HMCTS Guidance](#hmcts-guidance) for the central policy reference.

## HMCTS Guidance

- [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
- Use that guidance as the reference point for service-level issuer decisions and configuration recommendations.

## Quick Reference

| Topic | Current repo position |
| --- | --- |
| Validation model | Primary configured issuer plus optional additional issuers |
| Current service position | Explicitly enforced legacy `FORGEROCK` issuer |
| Discovery source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Default enforced issuer | `oidc.issuer` / `OIDC_ISSUER` |
| Optional additional issuers | `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` |
| Runtime rule | Always configure `OIDC_ISSUER`; leave `OIDC_ALLOWED_ISSUERS` unset unless real accepted tokens come from more than one issuer |

## Current service position

- `ts-translation-service` is currently configured to enforce the legacy `FORGEROCK` issuer for deployed environments.
- That matches the current Helm and Jenkins configuration in this repo and keeps issuer validation explicit rather than inferred from discovery metadata.
- The runtime supports optional additional issuers, but this repo currently leaves `OIDC_ALLOWED_ISSUERS` unset because the service is operating in single-issuer mode today.
- Only set `OIDC_ALLOWED_ISSUERS` after verifying this service receives valid tokens from more than one issuer; do not remove `OIDC_ISSUER` when adding extra issuers.

## Migration to IDAM issuer

- Moving this service to `IDAM` is an upstream configuration change, not a code-path change in this repo.
- If this service is moved to `IDAM`, the prerequisite issuer-policy update will be in the upstream `idam-access-config` repository, typically by setting `oauth2.required_issuer: IDAM` for this service there.
- After that upstream change, this repo’s `OIDC_ISSUER` values in Helm, preview config, and Jenkins must be updated to the new token issuer and verified against a real token.
- If AAT or preview must temporarily accept both public IDAM and ForgeRock issuers during migration, set `OIDC_ISSUER=https://idam-web-public.aat.platform.hmcts.net/o` and `OIDC_ALLOWED_ISSUERS=https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts`.
- Until that upstream change is made, the correct compliant behavior in this repo is to validate `iss` against the explicitly configured legacy `FORGEROCK` issuer rather than disable issuer checks or guess from discovery.

## Previous state

- The service previously used a custom validator chain containing only `JwtTimestampValidator`.
- The active decoder did not retain a `JwtIssuerValidator`.
- A JWT with a valid signature from the configured JWKS and valid timestamps could therefore be accepted without checking whether `iss` matched the intended issuer.

## Current implementation

- `src/main/java/uk/gov/hmcts/reform/translate/config/SecurityConfiguration.java` now validates both timestamp and issuer.
- Issuer validation is enforced with `JwtClaimValidator` against the configured issuer set.
- When `OIDC_ALLOWED_ISSUERS` is set to a comma-separated list, the active decoder accepts `OIDC_ISSUER` plus those additional issuers.
- The current deployed behavior remains single-issuer because `OIDC_ALLOWED_ISSUERS` is unset.

## Configuration meaning

| Setting | Purpose |
| --- | --- |
| `spring.security.oauth2.client.provider.oidc.issuer-uri` | OIDC discovery metadata and JWKS resolution |
| `oidc.issuer` | Primary issuer value always enforced |
| `oidc.allowed-issuers` | Optional comma-separated additional issuers for confirmed multi-issuer deployments |

## Test and build coverage

| Area | Coverage |
| --- | --- |
| `src/test/java/uk/gov/hmcts/reform/translate/config/SecurityConfigurationTest.java` | Focused validator chain behaviour, including primary issuer, additional issuer, blank, duplicate, and unexpected issuer cases |
| `src/integrationTest/java/uk/gov/hmcts/reform/translate/config/JwtDecoderIssuerValidationIT.java` | Active decoder accepts correctly signed tokens from configured issuers and rejects the same key material with an unexpected issuer |
| `src/functionalTest/java/uk/gov/hmcts/reform/translate/JwtIssuerVerificationApp.java` | Acquires a real BEFTA test token, decodes `iss`, and verifies it matches `OIDC_ISSUER` or an additional allowed issuer when enabled |
| `build.gradle` | Wires `verifyFunctionalTestJwtIssuer` into `smoke` and `functional`, gated by `VERIFY_OIDC_ISSUER=true` |

## CI and deployment requirement

- `VERIFY_OIDC_ISSUER=true` keeps the verifier mandatory in CI and opt-in locally.
- Jenkins must export `OIDC_ISSUER` explicitly because the verifier reads process environment, not Helm-rendered runtime env inside the deployed pod. If `OIDC_ALLOWED_ISSUERS` is configured for migration, Jenkins must export that value too.
- `OIDC_ISSUER` must stay aligned with the real token issuer for each environment.
- If `OIDC_ALLOWED_ISSUERS` is configured for a true multi-issuer deployment, the verifier accepts a real token issuer when it matches `OIDC_ISSUER` or appears in that comma-separated allow-list.
- Use [HMCTS Guidance](#hmcts-guidance) as the central policy reference for service-level issuer decisions.

## How to derive `OIDC_ISSUER`

- Do not guess the issuer from the public discovery URL alone.
- Decode only the JWT payload from a real access token for the target environment and inspect the `iss` claim.
- Do not store or document full bearer tokens. Record only the derived issuer value.

Example:

```bash
TOKEN='eyJ...'
PAYLOAD=$(printf '%s' "$TOKEN" | cut -d '.' -f2)
python3 - <<'PY' "$PAYLOAD"
import base64, json, sys
payload = sys.argv[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))["iss"])
PY
```

- JWTs are `header.payload.signature`.
- The second segment is base64url-encoded JSON.
- This decodes the payload only. It does not verify the signature.

## Outcome

- Prevents validly signed tokens from an unexpected issuer being accepted.
- Keeps discovery and enforcement semantics explicit.
- Makes issuer misconfiguration visible during build verification rather than only at runtime.

## Acceptance Checklist

Before merging JWT issuer-validation changes, confirm all of the following:

- The active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The active validator chain includes timestamp validation and issuer validation for `OIDC_ISSUER`, plus any `OIDC_ALLOWED_ISSUERS` when intentionally configured.
- There is no disabled, commented-out, or alternate runtime path that leaves issuer validation off.
- `issuer-uri` is used for discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is always used as an enforced token `iss` value, even when `OIDC_ALLOWED_ISSUERS` is intentionally configured.
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL.
- `OIDC_ALLOWED_ISSUERS`, if set, contains only additional issuer values verified from real tokens accepted by this service.
- App config, Helm values, preview values, and CI/Jenkins values are aligned for the target environment.
- If `OIDC_ISSUER` changed, it was verified against a real token for the target environment.
- There is a test that accepts a token with the expected issuer.
- There is a test that rejects a token with an unexpected issuer.
- There is a test that rejects an expired token.
- There is decoder-level coverage using a signed token, not only validator-only coverage.
- At least one failure assertion clearly proves issuer rejection, for example by checking for `iss`.
- CI or build verification checks that a real token issuer matches `OIDC_ISSUER`, or the repo documents why that does not apply.
- Comments and docs do not describe the old insecure behavior.
- Any repo-specific difference from peer services is intentional and documented.

Do not merge if any of the following are true:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- only happy-path tests exist

## Configuration Policy

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is the primary enforced JWT issuer and must match the token `iss` claim exactly.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` is optional, additive, and must remain unset unless this service receives tokens from more than one issuer.
- Do not derive `OIDC_ISSUER` from `IDAM_OIDC_URL` or the discovery URL.
- Production-like environments must provide `OIDC_ISSUER` explicitly.
- Main runtime config requires explicit `OIDC_ISSUER` with no static fallback.
- Local or test-only fallbacks are acceptable only when they are static, intentional, and clearly scoped to non-production use.
- The build enforces this policy with `verifyOidcIssuerPolicy`, which fails if `oidc.issuer` is derived from discovery config.

## References

- [HMCTS Guidance](#hmcts-guidance)
