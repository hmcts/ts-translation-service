# JWT Issuer Skill

Use this skill for JWT issuer validation, OIDC discovery vs enforced issuer, Helm/Jenkins issuer wiring, and related regression-test work in this repo.

## Workflow

1. Check current local diffs first so in-flight issuer-validation work is resumed instead of overwritten.
2. Treat `spring.security.oauth2.client.provider.oidc.issuer-uri` as discovery metadata and `oidc.issuer` as the enforced JWT issuer for the active decoder.
3. When changing issuer validation, update:
   - runtime config in `src/main/resources/application.yaml`
   - active decoder wiring in `src/main/java/uk/gov/hmcts/reform/translate/config/SecurityConfiguration.java`
   - focused unit coverage in `src/test/java/uk/gov/hmcts/reform/translate/config/`
   - active-decoder integration coverage in `src/integrationTest/java/uk/gov/hmcts/reform/translate/config/`
4. For smoke/functional test issuer verification, use a real BEFTA-acquired token, decode its `iss`, and compare it to `OIDC_ISSUER`.
5. Keep the build verifier gated by `VERIFY_OIDC_ISSUER=true` so it stays mandatory in CI and opt-in for local runs.
6. If the verifier reads process env, ensure Jenkins exports `OIDC_ISSUER` explicitly; Helm env values alone are not visible to that verifier step.
7. Update docs whenever discovery and enforced issuer behavior changes.

## Repo-specific notes

- Functional tests are under `src/functionalTest`.
- Helm values are under `charts/ts-translation-service/`.
- CI wiring is in `Jenkinsfile_CNP`.
