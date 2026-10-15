# CCD-7882: controlled error responses

API responses no longer include exception class names or raw exception messages. Standard HTTP status descriptions replace diagnostic messages. Existing fixed validation messages remain where already defined. Payment validation details contain user-facing validation messages, never exception causes. Advice error bodies include a stable category `code` derived only from the HTTP status, for example `INVALID_REQUEST` or `INTERNAL_ERROR`. Advice/security-handler error/warn logs contain the same code without exception messages or stack traces. Original exceptions are logged only at DEBUG, which can be enabled for the advice/security handler package when needed; log access must stay restricted through the existing platform controls. No existing non-identifying reference was found in these handlers, so no reference is added. Empty security responses and Spring Boot fallback responses retain their existing shapes.

Spring Boot fallback responses explicitly exclude exception, message, binding errors and stack traces, including when a caller requests them with query parameters. HTML error responses use a fixed generic page. Invalid bearer credentials return an empty 401; OAuth invalid-request errors retain 400. Both use a generic `WWW-Authenticate: Bearer` challenge; denied access returns 403 without diagnostic text. Token validation, endpoint permissions and document access checks are unchanged.

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
401/403 bodies with the code in handler logs. Spring Boot fallback responses retain their existing shape.
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
- Third-party ServiceAuthFilter and unrelated audit/operational logging are outside these handler changes;
  do not interpret handler log tests as a guarantee that all application/dependency logs are sanitized.

Functional tests require the services and dependencies used by BEFTA. Fixture updates and local unit
checks are not a deployed functional-test pass; run the functional suites in the target test environment.

For those additional owned paths, temporarily add the relevant logger entries alongside the earlier
DEBUG configuration, with the same App Insights threshold, then restore previous values:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter: DEBUG
```

Review-fix validation: **84 tests passed**, no failures or skips; Checkstyle and functional-test
Java compilation/resource processing passed. **4 functional JSON fixtures** were updated and
parsed successfully. Functional suites were not executed against deployed services.
