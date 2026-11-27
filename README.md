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

### Smoke and functional JWT issuer verification

The smoke and functional test tasks run a verifier when `VERIFY_OIDC_ISSUER=true`. The verifier fetches a real functional test token, decodes its `iss` claim, and checks it against the configured issuer values.

To enable the verifier locally, export:

```bash
  export VERIFY_OIDC_ISSUER=true
  export OIDC_ISSUER=<expected-token-iss>
```

Example for the current deployed ForgeRock issuer:

```bash
  export VERIFY_OIDC_ISSUER=true
  export OIDC_ISSUER=https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

Required settings:

| Setting | Meaning |
| --- | --- |
| `VERIFY_OIDC_ISSUER` | Set to `true` to run the verifier. When unset or false, the verifier is skipped. |
| `OIDC_ISSUER` | Required when the verifier runs. Must exactly match the token `iss` claim expected for the target environment. |

Optional settings:

| Setting | Meaning |
| --- | --- |
| `OIDC_ALLOWED_ISSUERS` | Optional comma-separated list of additional accepted issuer values. Leave unset unless this service is verified to receive valid tokens from more than one issuer. |

Example for a verified temporary multi-issuer migration:

```bash
  export OIDC_ISSUER=https://idam-web-public.aat.platform.hmcts.net/o
  export OIDC_ALLOWED_ISSUERS=https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

Configuration reference:

| Setting | Meaning |
| --- | --- |
| `spring.security.oauth2.client.provider.oidc.issuer-uri` | OIDC discovery and JWKS lookup only. Do not use it as a shortcut for the enforced token issuer. |

Current deployed environments enforce the explicitly configured legacy `FORGEROCK` issuer. If this service is moved to `IDAM`, the prerequisite issuer-policy change is in the upstream `idam-access-config` repository. After that change, this repo's `OIDC_ISSUER` values must be updated to match the new token `iss`.

To confirm the expected issuer from a failing request, decode only the JWT payload and inspect the `iss` claim. Do not commit or document full bearer tokens; record only the derived issuer value.

### Codex Workflow Docs

Repo-local workflow docs are indexed in `AGENTS.md`.

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
