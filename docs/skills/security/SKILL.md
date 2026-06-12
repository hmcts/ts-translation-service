# Security Skill

Use this skill for general security work in this repo, including JWT, IDAM, Spring Security, auth configuration, and security regression coverage.

## Workflow

1. Check current local diffs first so in-flight security work is resumed instead of overwritten.
2. Identify the actual security surface involved before editing:
   - runtime security config under `src/main/java/uk/gov/hmcts/reform/translate/config/`
   - auth-related properties under `src/main/resources/`
   - deployment and CI auth wiring under `charts/` and `Jenkinsfile_CNP`
   - existing unit, integration, and functional coverage under `src/test`, `src/integrationTest`, and `src/functionalTest`
3. Keep code, config, tests, and docs aligned for the security change you make.
4. If the task is specifically about JWT issuer validation, OIDC discovery vs enforced issuer, or the BEFTA issuer verifier, follow `docs/skills/security-jwt-issuer/SKILL.md`.

## Repo-specific notes

- Functional tests are under `src/functionalTest`.
- Helm values are under `charts/ts-translation-service/`.
- CI wiring is in `Jenkinsfile_CNP`.
