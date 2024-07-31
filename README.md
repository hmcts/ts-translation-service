# ts-translation-service

## Getting Started
This is the Translation Microservice.

Provides a capability for service users to maintain Welsh translations.

This enables Welsh Language Support for Professional Users; allowing EXUI users to choose to view the UI in Welsh

### Prerequisites

- [JDK 17](https://java.com)
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
  az acr login --name hmctspublic --subscription DCD-CNP-DEV
  az acr login --name hmctspublic --subscription DCD-CFT-Sandbox
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


