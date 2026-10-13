# CCD-7877 Hardcoded Credentials

## Objective

Ensure Translation Service database and S2S credentials are supplied at runtime.

## Acceptance criteria

- Database username/password and S2S secret have no committed credential defaults.
- Existing environment variable names and chart secret mappings remain the integration points.
- Local Docker receives database values from the caller’s environment or an untracked local `.env`.

## Findings and changes

- Removed `postgres` database credential defaults from `src/main/resources/application.yaml`; use `TS_TRANSLATION_SERVICE_DB_USERNAME` and `TS_TRANSLATION_SERVICE_DB_PASSWORD`.
- Removed the committed `AAAAAAAAAAAAAAAA` S2S default; use `IDAM_KEY_TS_TRANSLATION_SERVICE`.
- Preview database configuration now receives its username and password through external substitution rather than `hmcts` defaults.
- The tracked `.env` contains only `SERVER_PORT`; no secret value was identified there.
- Live validity, deployment, reuse, and rotation status cannot be established locally.

## Local validation

Set `TS_TRANSLATION_SERVICE_DB_NAME`, `TS_TRANSLATION_SERVICE_DB_USERNAME`, and `TS_TRANSLATION_SERVICE_DB_PASSWORD` before running `docker compose up`, then run the existing Gradle tests.

## Recommendations

Use the service secret store for deployed values and rotate any historical credentials represented by the removed defaults.
