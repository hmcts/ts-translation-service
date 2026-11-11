# CCD-7882 error contract

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
telemetry and DEBUG ingestion with application/agent DEBUG enabled. Production configuration and
deployed consumer contracts require environment-specific checks.
