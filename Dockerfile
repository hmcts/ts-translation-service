# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.9
ARG PLATFORM=""

FROM hmctsprod.azurecr.io/base/java${PLATFORM}:21-distroless
USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/ts-translation-service.jar /opt/app/

EXPOSE 4650
CMD [ "ts-translation-service.jar" ]
