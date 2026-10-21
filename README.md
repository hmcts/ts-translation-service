# ts-translation-service

## Getting Started
This is the Translation Microservice.

Provides a capability for service users to maintain Welsh translations.

This enables Welsh Language Support for Professional Users; allowing EXUI users to choose to view the UI in Welsh

### Prerequisites

- [JDK 21](https://java.com)
- [Docker](https://www.docker.com)

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

The easiest way to run the application locally is to use the `bootWithCCD` Gradle task.

**Run the application**

Run the application by executing the following command:

```bash
./gradlew bootWithCCD
```

This will start the application and its dependent services.

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4650/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":67371577344,"free":42536177664,"threshold":10485760,"exists":true}},"ping":{"status":"UP"}}}
```

Should the docker containers fail to start, it is likely that the `bootWithCCD` plugin is not authorized to pull the container images from Azure.

Log in, using the commands below

```bash
  az acr login --name hmctsprod --subscription DCD-CNP-DEV
  az acr login --name hmctsprod --subscription DCD-CFT-Sandbox
```

### Alternative to running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/ts-translation-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4650` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4650/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":67371577344,"free":42536177664,"threshold":10485760,"exists":true}},"ping":{"status":"UP"}}}
```

### Other

Hystrix offers much more than Circuit Breaker pattern implementation or command monitoring.
Here are some other functionalities it provides:
 * [Separate, per-dependency thread pools](https://github.com/Netflix/Hystrix/wiki/How-it-Works#isolation)
 * [Semaphores](https://github.com/Netflix/Hystrix/wiki/How-it-Works#semaphores), which you can use to limit
 the number of concurrent calls to any given dependency
 * [Request caching](https://github.com/Netflix/Hystrix/wiki/How-it-Works#request-caching), allowing
 different code paths to execute Hystrix Commands without worrying about duplicating work

## Error handling and support

JSON error responses include stable string codes to help support identify the error category.
A code describes the HTTP status; it does not identify an individual incident or establish its cause.

| HTTP status | Code |
| --- | --- |
| 400, 422 | `INVALID_REQUEST` |
| 401 | `AUTHENTICATION_REQUIRED` |
| 403 | `ACCESS_DENIED` |
| 404 | `RESOURCE_NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` |
| 413 | `REQUEST_TOO_LARGE` |
| 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 429 | `TOO_MANY_REQUESTS` |
| 5xx | `INTERNAL_ERROR` |
| Other errors | `REQUEST_FAILED` |

Authentication and service-authentication filters return empty 401/403 bodies and record the category
in handler logs. Controller errors can return JSON bodies. Fallback JSON and HTML error pages may
omit the code. Normal WARN/ERROR failure logs retain exception details for administrator support; public responses remain generic.

### Investigating a failure

Record the service/environment, UTC time, HTTP status, error code where available and an existing
telemetry operation ID. Use the operation ID and time to find related requests and logs in App Insights.
No additional incident reference is generated. Do not request bearer tokens or credentials from users.

For 400/422, check the request format and validation messages. For 401, check sign-in; for 403, check
permissions. Escalate unexpected 5xx failures with the recorded context. Detailed logs should be
accessible only through the environment's restricted administrator/support access.

### Optional additional DEBUG diagnostics

Original failure exceptions are already retained at normal WARN/ERROR levels. For additional tracing, enable DEBUG only for the logger needed. For request or authentication
failures, use the relevant entry below in the deployment's logging configuration:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.translate.errorhandling.RestExceptionHandler: DEBUG
    uk.gov.hmcts.reform.translate.security.SafeAuthenticationErrors: DEBUG
```

Other diagnostic loggers are:

- `uk.gov.hmcts.reform.translate.security.SafeServiceAuthFilter` for service authentication.
- `uk.gov.hmcts.reform.translate.logging.SafeHealthDiagnosticsFilter` for health checks.
- `uk.gov.hmcts.reform.translate.logging.SafeDependencyDiagnosticsFilter` for database/framework diagnostics.

Tomcat can rate-limit malformed HTTP request diagnostics. For repeated parser failures, temporarily
enable `org.apache.coyote.http11.Http11Processor: DEBUG` as well; restore its previous level afterwards.

If using the Application Insights Java agent, its logging collection threshold must also allow DEBUG:

```text
APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL=DEBUG
```

Apply startup configuration through the normal deployment process and restart/roll out as required.
Reproduce the failure and check related trace/exception telemetry using the operation ID. Additional DEBUG events are available only while DEBUG is enabled; normal failure diagnostics remain available subject to collection and retention.
Restore both logger and collection thresholds afterwards. Server diagnostics can contain sensitive
information and must remain subject to restricted log access.

### Preview Admin Web startup

The preview Helm values mount the CCD Key Vault's `session-keys` secret for Admin Web. Its startup
validation requires a JSON array containing at least two non-empty signing keys at
`secrets.ccd.session-keys`; without it, the pod exits before serving requests. Keep the secret available
to the preview deployment and share the configured keys across replicas. Key values belong in Key Vault.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
