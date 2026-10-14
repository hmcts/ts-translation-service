# CCD-7882: controlled error responses

API responses no longer include exception class names or raw exception messages. Standard HTTP status descriptions replace diagnostic messages. Existing fixed validation messages remain where already defined. Payment validation details contain user-facing validation messages, never exception causes. Detailed exceptions remain in server logs; log access must stay restricted through the existing platform controls.

Spring Boot fallback responses explicitly exclude exception, message, binding errors and stack traces, including when a caller requests them with query parameters. HTML error responses use a fixed generic page. Invalid bearer authentication returns an empty 401 with a generic `WWW-Authenticate: Bearer` challenge; denied access returns 403 without diagnostic text. Token validation, endpoint permissions and document access checks are unchanged.

## Verification and release handoff

Local regression tests cover diagnostic redaction, malformed JSON, invalid UUID conversion, unsupported media/method status codes, and authentication error handlers. Existing relevant tests are also run. These checks do not establish deployment or end-to-end acceptance.

At merge/deployment planning, assign the delivery owner, QA contact, target date and retest environment. After deployment, record the actual revision, environment, UTC timestamp and sanitised response evidence in the tracking ticket. Do not record credentials, document contents or the OFFICIAL-SENSITIVE report. Obtain detailed reproduction material from SecOps through the approved channel.

## Local verification — 9 October 2026

Branch: `CCD-7882_Verbose_Error_Messages`, based on master `bdf53b0`. Results apply to the local working changes; they are not deployment evidence.

```sh
./gradlew test --offline --tests 'uk.gov.hmcts.reform.translate.errorhandling.*' --tests 'uk.gov.hmcts.reform.translate.security.*' --tests 'uk.gov.hmcts.reform.translate.config.*' checkstyleMain checkstyleTest --console=plain
```

Result: **79 tests passed**, no failures or skips; `checkstyleMain` and `checkstyleTest` passed. Test reports are generated under `build/reports/tests/test/` and JUnit XML under `build/test-results/test/`.
