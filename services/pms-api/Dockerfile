# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/pms-api -am -DskipTests package

FROM scratch AS appinsights-agent
ADD --checksum=sha256:4ab7a442bf9defc7475d026c6b49042793ebb010c391a1db91ae07da0c04d848 \
    https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.9/applicationinsights-agent-3.7.9.jar \
    /applicationinsights-agent.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=appinsights-agent --chown=app:app /applicationinsights-agent.jar /app/applicationinsights-agent.jar
COPY --from=build --chown=app:app services/pms-api/target/pms-api-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8081
ENV APPLICATIONINSIGHTS_ROLE_NAME=pms-api
ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-jar", "/app/app.jar"]
