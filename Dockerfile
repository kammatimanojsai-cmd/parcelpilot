# The pipeline builds and tests the jar first; this image only packages the tested artifact.
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/parcelpilot.jar app.jar
USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
