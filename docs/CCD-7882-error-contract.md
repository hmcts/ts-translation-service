# CCD-7882: controlled error responses

API responses no longer include exception class names or raw exception messages. Standard HTTP status descriptions replace diagnostic messages. Existing fixed validation messages remain where already defined. Payment validation details contain user-facing validation messages, never exception causes. Advice error bodies include a stable category `code` derived only from the HTTP status, for example `INVALID_REQUEST` or `INTERNAL_ERROR`. Advice/security-handler error/warn logs contain the same code without exception messages or stack traces. Original exceptions are logged only at DEBUG, which can be enabled for the advice/security handler package when needed; log access must stay restricted through the existing platform controls. No existing non-identifying reference was found in these handlers, so no reference is added. Empty security responses retain their existing shapes. Advice and Spring Boot fallback JSON omit reflected request paths, which can contain identifiers.

Spring Boot fallback responses explicitly exclude exception, message, binding errors and stack traces, including when a caller requests them with query parameters. HTML error responses use a fixed generic page. Invalid bearer credentials return an empty 401; OAuth invalid-request errors retain 400. Bearer challenges retain only the scheme and recognised protocol error codes (`invalid_request`, `invalid_token`, `insufficient_scope`), with no descriptions, URIs or scopes. Missing credentials use `WWW-Authenticate: Bearer`. Denied bearer access retains 403 and its safe challenge; S2S denials remain empty 401/403 without a bearer challenge. Token validation, endpoint permissions and document access checks are unchanged.

## Verification and release handoff

Local regression tests cover diagnostic redaction, malformed JSON, invalid UUID conversion, unsupported media/method status codes, and authentication error handlers. Existing relevant tests are also run. These checks do not establish deployment or end-to-end acceptance.

At merge/deployment planning, assign the delivery owner, QA contact, target date and retest environment. After deployment, record the actual revision, environment, UTC timestamp and sanitised response evidence in the tracking ticket. Do not record credentials, document contents or the OFFICIAL-SENSITIVE report. Obtain detailed reproduction material from SecOps through the approved channel.

## Local verification — 9 October 2026

Branch: `CCD-7882_Verbose_Error_Messages`, based on master `bdf53b0`. Results apply to the local working changes; they are not deployment evidence.

```sh
./gradlew test --offline --tests 'uk.gov.hmcts.reform.translate.errorhandling.*' --tests 'uk.gov.hmcts.reform.translate.security.*' --tests 'uk.gov.hmcts.reform.translate.config.*' checkstyleMain checkstyleTest --console=plain
```

Result: **79 tests passed**, no failures or skips; `checkstyleMain` and `checkstyleTest` passed. Test reports are generated under `build/reports/tests/test/` and JUnit XML under `build/test-results/test/`.

## Support use of error codes

HTTP status remains unchanged. Advice JSON includes `code`; security filters continue to return empty
401/403 bodies with the code in handler logs. Spring Boot fallback responses retain their existing fields except the reflected `path`.
These backend changes do not make a frontend display the code automatically.

| Status | Code | Support action |
| --- | --- | --- |
| 400 / 422 | INVALID_REQUEST | Check request format and safe validation messages; correct input. |
| 401 | AUTHENTICATION_REQUIRED | Check sign-in/session; never collect bearer tokens. |
| 403 | ACCESS_DENIED | Check the user's intended permissions; escalate unexpected refusals. |
| 5xx | INTERNAL_ERROR | Escalate with service, time, status and available telemetry correlation. |

Other existing statuses retain distinct codes: RESOURCE_NOT_FOUND (404), METHOD_NOT_ALLOWED (405),
REQUEST_TOO_LARGE (413), UNSUPPORTED_MEDIA_TYPE (415), TOO_MANY_REQUESTS (429).
Other statuses use REQUEST_FAILED. Codes describe categories, not a confirmed root cause. They are defined as `ErrorCode` enum constants
in Java and serialized as the same strings in JSON; HTTP status remains numeric.

Local log-capture regression tests verify these examples at INFO (synthetic requests):

```text
ERROR Request failed: code=INVALID_REQUEST status=400
WARN  Authentication failed: code=AUTHENTICATION_REQUIRED status=401
WARN  Access denied: code=ACCESS_DENIED status=403
ERROR Request failed: code=INTERNAL_ERROR status=500
```

At DEBUG, an additional event contains the same category/status and original exception with stack trace.
Normal handler events contain no exception message or throwable. Collect service/environment, UTC time,
status, code and an existing telemetry operation ID if available. A category is shared by many requests
and cannot identify one incident. No new incident reference is added.

Enable DEBUG temporarily for the advice and security handler loggers only, then restore the previous level.
Diagnostic logs can contain sensitive details; retain existing restricted access. Local tests establish
handler output, not App Insights ingestion, severity filters, correlation or independent agent exception
collection. Validate those in the target environment before relying on the examples in App Insights.

## Local verification — 15 October 2026

Meaningful codes and INFO/DEBUG log capture: **82 focused tests passed**, no failures or skips.
The focused suites and Checkstyle commands above were rerun. This is local evidence, not deployment
or App Insights evidence. Changes remain uncommitted.

## Temporarily enabling diagnostics in App Insights

The original exception message and stack trace are passed to `log.debug(..., exception)`.
Normal ERROR/WARN events contain only the fixed category and HTTP status. Diagnostics are
available only for failures occurring while DEBUG is enabled; earlier details cannot be recovered.

Two thresholds must allow DEBUG:

1. Override Spring logging for these two handler classes in the target environment:

   ```yaml
   logging:
     level:
       uk.gov.hmcts.reform.translate.errorhandling.RestExceptionHandler: DEBUG
       uk.gov.hmcts.reform.translate.security.SafeAuthenticationErrors: DEBUG
   ```

   Merge these entries with the existing `logging.level` configuration. Keep other application
   loggers at their existing levels; do not enable root DEBUG.

2. Allow the Application Insights Java 3.x agent to collect DEBUG events:

   ```text
   APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL=DEBUG
   ```

   The agent's default collection threshold is INFO. Changing Spring logging alone is insufficient.
   This agent threshold applies to all emitted logs, so review other loggers already set to DEBUG.
   It is not the agent's `selfDiagnostics` setting.

Apply these temporary overrides through the environment's normal deployment process; a restart/rollout
is needed when supplying startup configuration. Confirm the agent is attached and the connection-string
secret referenced by `lib/applicationinsights.json` is mounted. Existing repository configuration does
not establish that a deployed environment has these settings.

Reproduce a controlled failure. With the Java 3.x agent, normal events appear in `traces`, while events
carrying an exception object appear in `exceptions`. In the Application Insights Logs view, for example:

```kusto
union traces, (exceptions | extend message = outerMessage)
| where timestamp > ago(30m)
| where cloud_RoleName == "ts-translation-service"
| where message contains "code="
| project timestamp, operation_Id, severityLevel, message, itemType
| order by timestamp desc
```

Use `operation_Id` to inspect related request telemetry when correlation is available. In a
workspace table view, use the equivalent `AppTraces`/`AppExceptions` schema. Check ingestion,
filters and sampling if expected events are missing; local log-capture tests do not validate these.

Restore both thresholds to their previous values after diagnosis and confirm DEBUG events stop.
Original diagnostics may contain sensitive details; use existing restricted log access.
No deployed logging settings were changed by this documentation update.

Reference: [Microsoft Java agent logging configuration](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config#autocollect-logging).

## Senior review follow-up

- Preserve OAuth bearer error statuses: an invalid bearer request remains 400 with INVALID_REQUEST
  in handler logs; authentication failures remain 401. Bearer challenges and bodies contain no diagnostics.
- Move original diagnostics from the owned IDAM/document-client/filter failure paths to DEBUG.
  Keep generic operational events at ERROR/WARN. This does not change HTTP status or authentication checks.
- Update functional response fixtures to assert safe codes/messages instead of exception classes,
  internal messages or identifiers. Keep existing status expectations and fixed user-facing validation details.
- The shared ServiceAuthFilter is replaced with an owned filter using the same validator/allowlist
  and 401/403 semantics. Audit failure diagnostics and operational identifiers are available only at DEBUG.

Functional tests require the services and dependencies used by BEFTA. Fixture updates and local unit
checks are not a deployed functional-test pass; run the functional suites in the target test environment.

For those additional owned paths, temporarily add the relevant logger entries alongside the earlier
DEBUG configuration, with the same App Insights threshold, then restore previous values:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.security.SafeAuthenticationErrors: DEBUG
```

Review-fix validation: **84 tests passed**, no failures or skips; Checkstyle and functional-test
Java compilation/resource processing passed. **4 functional JSON fixtures** were updated and
parsed successfully. Functional suites were not executed against deployed services.

## Complete request-path fixes — 16 October 2026

Public health endpoints retain their availability and status codes but use `show-details: never`,
so failed indicators cannot publish exception text through health JSON. Detailed audit exceptions,
service-authentication exceptions and operational user/case/document/service identifiers are moved
from ERROR/WARN into DEBUG; normal events use generic summaries or fixed codes.

The shared S2S auto-configuration bean (`serviceAuthFiler`) is replaced after initialization by
`SafeServiceAuthFilter`, retaining the same ServiceAuthTokenValidator implementation and configured service allowlist,
Bearer normalization, empty 401/403 responses and downstream exception handling semantics.
Filter overrides with distinct bean names remain intact. This wiring depends on the shared bean name and
has a regression test; review it when upgrading the service-auth library.

Request parsing and general conversion failures retain 400/INVALID_REQUEST. Response-writing failures
(HttpMessageNotWritableException) retain 500/INTERNAL_ERROR through Spring's more specific inherited
handler. An executable comparison with the pre-change advice confirmed this distinction. Once a
response is committed, its status/body cannot be replaced; deployment checks must include aborted
responses and telemetry behaviour.

For temporary diagnosis, enable DEBUG only for the relevant owned security, audit, controller or
service logger, together with the App Insights collection threshold described above. Restore both
thresholds after diagnosis. Full deployed BEFTA execution and App Insights ingestion remain
separate release checks. These changes do not promise that every third-party/framework logger
will suppress diagnostics; those still require environment verification.

Additional logger overrides for the complete-path fixes (use only those needed):

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.security.SafeServiceAuthFilter: DEBUG
    uk.gov.hmcts.reform.translate.service.DictionaryService: DEBUG
```

### Verified local results — 16 October 2026

**122 affected tests passed**, zero failures/errors/skips. `checkstyleMain`, `checkstyleTest`,
`compileIntegrationTestJava` and `compileFunctionalTestJava` passed. Regression coverage includes
public health UP/DOWN response redaction (200/503), S2S 401/403/allowlist/Bearer handling and actual
shared-bean replacement, INFO/DEBUG diagnostic separation, response serialization as 500 (confirmed against the original handler), and
preserved 405 Allow / 415 Accept headers. Translation/CPO also cover whole security-chain logging
and committed error handling; CPO/CDAM cover audit interceptor and SpEL failures with log capture.

Health functional fixtures now expect status without component diagnostics. Existing error fixtures
retain their HTTP statuses, including the original 500 for response-writing failures. Integration/functional
sources were compiled; deployed BEFTA execution, real interrupted downloads, secret mounts,
upstream gateways and App Insights capture/sampling still need environment verification.
Changes are uncommitted; no deployment configuration was applied to a running environment.

## Framework failure-path follow-up — 16 October 2026

Health indicators can log their own exceptions at WARN even when public health details are hidden.
`SafeHealthLoggingConfiguration` registers an owned Logback filter for health-indicator logger classes.
It replaces their WARN/ERROR events with a fixed summary, preserving severity, and routes original
messages/throwables to `logging.SafeHealthDiagnosticsFilter` at DEBUG. The filter is removed when
its Spring context closes. Health results and HTTP statuses are unchanged. This application uses
Logback; custom contributors that log through unrelated logger names need separate verification.

Bearer authentication now has an explicit failure handler. AuthenticationServiceException (including
IDAM outages) remains 500, with an empty body and no Bearer challenge. The original exception is
logged at DEBUG only, preventing Spring's default rethrow from reaching container ERROR logs.
Normal invalid-token/invalid-request handling continues through the existing safe entry point.

For temporary diagnosis, add the following alongside the previously documented App Insights
collection threshold, then restore the prior level:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.logging.SafeHealthDiagnosticsFilter: DEBUG
```

IDAM failure diagnostics use the existing `uk.gov.hmcts.reform.translate.security.SafeAuthenticationErrors` DEBUG setting.

Framework-path regression results: **124 affected tests passed**, no failures/errors/skips.
Tests now use real AbstractHealthIndicator implementations (including nested composite contributors)
and capture propagated INFO/DEBUG logs. Translation/CPO also simulate IDAM failures through the
full bearer chain, verifying empty 500 responses, no WWW-Authenticate header, no normal-level
throwables, and DEBUG-only causes. Public health status/body and existing bearer checks remain green.
Checkstyle and integration/functional compilation are checked separately; deployed functional tests
and App Insights ingestion are still outstanding.

## Branch review corrections — 16 October 2026

- Override Spring's method-not-supported handler to avoid its original diagnostic at WARN.
  The response remains 405/METHOD_NOT_ALLOWED and retains the Allow header. A propagated-log
  regression uses a synthetic private method name to check both logs and response redaction.
- Respect each health indicator logger's effective level before re-emitting a safe summary.
  OFF suppresses all events; ERROR suppresses WARN; permitted ERROR/WARN events retain severity.
- Log IDAM failures once at the authentication boundary. The converter only preserves and rethrows
  the cause; the boundary emits one generic ERROR and, when enabled, one DEBUG diagnostic.
  Existing converter-only DEBUG instructions no longer produce failure diagnostics; enable
  SafeAuthenticationErrors for IDAM diagnosis.

The shared S2S policy copy remains a dependency-upgrade concern. The follow-up below covers actual
shared auto-configuration registration and Logback reload. Deployed BEFTA and App Insights ingestion are not established by local checks.

Validation: **126 tests passed**, with no failures, errors or skips. Checkstyle and integration/functional
test Java compilation passed.


## Current-branch request/error review — 16 October 2026

The ticket description and final code-table comment were supplied in the conversation on
16 October 2026. They require generic responses, restricted diagnostic logs and unchanged HTTP
statuses. Scope of the completed implementation review below is this translation-service branch.

- Advice and Boot fallback JSON no longer reflect request paths. Java callers retain the existing
  `HttpError.getPath()` API. Annotation reason text is excluded; fixed validation messages remain.
  Four functional error fixtures remove only the `path` expectation, retaining their statuses.
- Bearer handlers retain recognised protocol error codes in `WWW-Authenticate`, including
  `insufficient_scope` for authenticated access denial. Diagnostic descriptions, URIs, scopes and
  arbitrary error codes are excluded. Existing token validation and bypass rules remain unchanged.
- `SafeDependencyDiagnosticsFilter` routes WARN/ERROR diagnostics from Hibernate, PostgreSQL,
  Hikari, Spring transactions/JPA, MVC fallback resolvers, DispatcherServlet's PageNotFound logger,
  health endpoint timing warnings and Tomcat request processing to its owned DEBUG logger.
  Normal events use fixed summaries without identifiers or throwables. JDBC, transaction-interceptor,
  TransactionTemplate and MVC resolver diagnostics defer the summary to the terminal boundary,
  avoiding repeated failure summaries. Connection-pool and container events retain severity.
- Real SqlExceptionHelper and TransactionTemplate regressions verify that original messages/causes
  remain available at DEBUG, including an application exception superseded by rollback failure.
  Unknown-URL tests cover both the DispatcherServlet warning and safe 404 response.
- Real shared S2S auto-configuration is exercised with Feign auto-configuration; its servlet
  registration remains disabled and references the safe filter. Logging context reset tests verify
  both filters are reinstalled without duplicates and removed on context shutdown.
- Existing unit/controller/repository integration suites continue to cover success, validation,
  authorization and concurrency paths. Integration validation responses now assert safe codes and
  absent path/exception/trace fields. Most integration HTTP tests deliberately disable filters;
  separate security-chain unit tests exercise authentication failures and IDAM outages.

For dependency diagnosis, temporarily enable the following alongside the App Insights collection
threshold documented above, then restore it:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.logging.SafeDependencyDiagnosticsFilter: DEBUG
```

Remaining environment checks: deployed BEFTA, real gateway/container error dispatch and interrupted
responses, App Insights collection/correlation/independent exception instrumentation, and logs emitted
before Spring installs the filters. Custom health indicators using unrelated logger names and future
dependency logger changes require separate verification. The local checks do not establish those paths.

Final validation: **152 unit tests and 52 PostgreSQL integration tests passed**, with no failures,
errors or skips. Checkstyle passed for main, unit, integration and functional sources; functional
Java compilation passed, all 52 functional JSON files parsed, and `git diff --check` passed.

```sh
./gradlew test integration --offline checkstyleMain checkstyleTest checkstyleIntegrationTest checkstyleFunctionalTest compileFunctionalTestJava --console=plain
```


## Ticket-description confirmation — 16 October 2026

Current branch status: general conversion errors retain 400/INVALID_REQUEST; response-writing errors
retain 500/INTERNAL_ERROR. Generic responses and DEBUG-only diagnostics remain in place, with
authentication statuses and required headers preserved. Deployed ITHC retesting and restricted log
access/App Insights verification remain outstanding.

All three checked-out branches are `CCD-7882_Verbose_Error_Messages`. Their ErrorCode enum bodies
and mappings are identical to the proposed table. Generic advice/HTML responses, disabled Boot
exception/message/trace/binding output and DEBUG diagnostics implement the main remediation.
This is a source/documentation review, not evidence of deployment, administrator-only log access,
retesting the reported ITHC URLs, or stakeholder approval of the shared code names. See the README
for the concise contract and unresolved branch-specific gaps. No implementation changes or test
reruns were made for this documentation confirmation.


## Translation status compatibility restored — 16 October 2026

An earlier review incorrectly treated all conversion subtypes as 400. The subsequent executable
baseline check below established that HttpMessageConversionException uses 400 while Spring's more
specific HttpMessageNotWritableException mapping uses 500. The writable-message 400 override has been
removed. General conversion failures remain safe 400/INVALID_REQUEST; response-writing failures use
safe 500/INTERNAL_ERROR. Original diagnostics remain at DEBUG, with headers and committed-response
handling preserved. No functional fixture changes are needed for this correction.

The earlier 34-test run checked the proposed 400 override, not parity with the original handler.
It is superseded by the baseline comparison and full-suite results below.


## Final requirement review and baseline comparison — 16 October 2026

A temporary standalone MockMvc probe compiled the original RestExceptionHandler from the revision
before the first controlled-error-response commit (`a080f50^`) against the current resolved dependencies.
It confirmed HttpMessageConversionException returned 400 and HttpMessageNotWritableException returned
500. The current handlers and regressions now preserve both outcomes; the earlier advice that response
writing should be restored to 400 was incorrect. The probe files live outside the repository.

Additional security-chain regressions exercise the actual `/dictionary` and `/translation/cy` routes:
missing S2S credentials remain empty 401 without a bearer challenge, dictionary reads still require user
authentication after S2S validation, translations retain service-only access, approved dictionary PUT
services still bypass user authentication, and malformed translation payloads return safe 400 JSON.
These tests use the real configured filters/controllers with mocked service boundaries; database and
role checks are covered separately by integration tests. No authentication configuration was changed.

A parameterized contract regression covers every proposed category and JSON string serialization.
INTERNAL_ERROR is limited to 500–599, with other unmapped statuses using REQUEST_FAILED. The names are
suitable non-identifying categories; stakeholder acceptance remains a separate decision.

The application-side implementation provides generic error responses/pages and owned DEBUG diagnostics.
Production acceptance still requires ITHC/BEFTA retesting, upstream error-page checks and verification
of administrator-only log access and App Insights ingestion/independent exception instrumentation.
No new incident reference is generated. The README contains support/maintenance guidance only.


Final validation: **173 unit tests and 52 PostgreSQL integration tests passed**, zero failures,
errors or skips. Main/unit/integration/functional Checkstyle passed; functional Java compilation,
functional JSON parsing and `git diff --check` passed. The complete unit run includes dependency-log,
health/reload, empty authentication responses, required headers, committed responses and duplicate
failure-summary regressions. Deployed functional suites were not run.

```sh
./gradlew test integration --offline checkstyleMain checkstyleTest checkstyleIntegrationTest checkstyleFunctionalTest compileFunctionalTestJava --console=plain
```
