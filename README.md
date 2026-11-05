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

Choose one of three local runtime modes. If `ccd-docker` is already running, use the
`bootRun` instructions below; this avoids starting or downloading a second CCD
environment. Otherwise, choose either `bootWithCCD` or standalone Docker
Compose.

Quick examples:

```bash
# ccd-docker is already running; use the detailed bootRun command below
# ./gradlew bootRun

# Start the application and CCD dependencies together
./gradlew bootWithCCD

# Run the standalone Docker Compose setup
./bin/run-in-docker.sh
```

Choose one mode only. Do not run `bootWithCCD` when `ccd-docker` is already
running.

When using `bootWithCCD`, check the application health in another terminal:

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

### Running with an existing `ccd-docker`

Use this mode when `ccd-docker` is already running. It starts only the local
translation-service process and reuses `ccd-docker`'s IDAM, S2S, and shared
PostgreSQL containers. Do not use `bootWithCCD` in this mode: that task starts
another CCD environment and uses the separate RSE database proxy on port `6432`.

From this repository, run:

```bash
CCD_DOCKER_DIR=../ccd-docker
[ -r "$CCD_DOCKER_DIR/.env" ] || { echo "Missing $CCD_DOCKER_DIR/.env"; exit 1; }
set -a
. "$CCD_DOCKER_DIR/.env"
set +a

TS_TRANSLATION_SERVICE_DB_HOST=localhost \
TS_TRANSLATION_SERVICE_DB_PORT=5050 \
TS_TRANSLATION_SERVICE_DB_NAME=ts_translation_service \
TS_TRANSLATION_SERVICE_DB_USERNAME="$DB_USERNAME" \
TS_TRANSLATION_SERVICE_DB_PASSWORD="$DB_PASSWORD" \
IDAM_KEY_TS_TRANSLATION_SERVICE="$IDAM_KEY_TS_TRANSLATION_SERVICE" \
IDAM_OIDC_URL=http://localhost:5000 \
IDAM_API_URL=http://localhost:5000 \
OIDC_ISSUER=http://localhost:5000/o \
S2S_URL=http://localhost:4502 \
./gradlew bootRun
```

The shared database is exposed by `ccd-docker` as host port `5050`; its
credentials must come from `ccd-docker/.env`. Do not use the standalone
Compose `.env.local` credentials with this database.

Check the service in another terminal:

```bash
curl http://localhost:4650/health
```

### Alternative to running the application

#### Run with Docker Compose

The local database username is `ts`; the password is generated and stored in the
ignored `.env.local` file. Generate it and start the application with:

```bash
./bin/run-in-docker.sh
```

The generator is safe to rerun: if `.env.local` already exists, its credentials
are preserved. This is important because the PostgreSQL data volume uses the
credentials from its first initialisation.

To generate the file without starting the containers:

```bash
./bin/generate-local-env.sh
```

Check that all required values are populated without printing the secrets:

```bash
for name in TS_TRANSLATION_SERVICE_DB_NAME TS_TRANSLATION_SERVICE_DB_USERNAME TS_TRANSLATION_SERVICE_DB_PASSWORD; do
  grep -Eq "^${name}=.+$" .env.local || { echo "Missing ${name}"; exit 1; }
done
docker compose --env-file .env.local config >/dev/null
```

Build the application distribution by executing:

```bash
  ./gradlew assemble
```

Build the Docker image:

```bash
  docker compose --env-file .env.local build
```

Run the Docker Compose application by executing:

```bash
docker compose --env-file .env.local up
```

This will start the API container exposing the application's port
(set to `4650` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
curl http://localhost:4650/health
```

The response should contain `"status":"UP"`. Container status and logs can be
checked with:

```bash
docker compose --env-file .env.local ps
docker compose --env-file .env.local logs ts-translation-service
```

Stop the containers while keeping the database volume with:

```bash
docker compose --env-file .env.local down
```

Do not use `down -v` unless you intentionally want to delete the local database.

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
