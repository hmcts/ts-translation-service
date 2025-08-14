# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.4
ARG PLATFORM=""

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/ts-translation-service.jar /opt/app/

EXPOSE 4650
CMD [ "ts-translation-service.jar" ]
