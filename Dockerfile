ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/ts-translation-service.jar /opt/app/

EXPOSE 4650
CMD [ "ts-translation-service.jar" ]
