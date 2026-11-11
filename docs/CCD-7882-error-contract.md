# CCD-7882 error contract

Related requirements: CCD-7108 and CME-984.

Prevent public errors from revealing exception classes, stack traces, internal URLs, file paths
or software details. Preserve response schemas, HTTP status mappings, approved business messages
and public `message` fields where present, while retaining useful private diagnostics.

## Responses

- Disable automatic exception messages and binding-error disclosure. Hide health details, replace
  fallback HTML with generic content and disable native Tomcat reports/version information.
- Keep `HttpError` fields with generic messages, `exception: "Exception"` and `path: "/"`. Preserve
  handler status mappings and required response headers; add no public error-code field.
- Sanitise user/service authentication failures and bearer challenges so decoder descriptions,
  service details and internal URLs are not returned.

## Diagnostics

Advice logs retain original exceptions at ERROR with private `failureCode` and `httpStatus`
dimensions. Authentication failures retain server diagnostics. Sensitive deletion details move to
DEBUG while a count remains at WARN. Correlation context is preserved.

## Verification

Focused response, authentication, health and diagnostic tests cover sanitisation, native log
categories and logging thresholds. Docker checks cover malformed requests, correlated failure
telemetry and DEBUG ingestion with application/agent DEBUG enabled.

## Support

In Application Insights, filter private `failureCode` and `httpStatus` dimensions, then use
`operation_Id` to find related requests and dependencies. Inspect exception details in restricted
telemetry or server logs; public error messages are sanitised. Failure diagnosis should work at
normal production logging levels without enabling DEBUG.

## Deployment checks

- Verify effective error/logging settings and sanitised responses in each environment. Confirm
  `server.error.include-message=never` at runtime.
- Confirm diagnostic logs and Application Insights are restricted to authorised personnel, with
  suitable sampling and retention.
- Verify failure codes and correlation IDs are searchable at normal production logging levels;
  run deployed consumer/Pact checks.
