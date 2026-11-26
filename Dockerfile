# syntax=docker/dockerfile:1
# Build from the backend root; scripts/docker/build-images.ps1 supplies the service/port pair.
ARG MAVEN_IMAGE=maven:3-eclipse-temurin-21-jammy
ARG JAVA_IMAGE=eclipse-temurin:21-jre-jammy
FROM ${MAVEN_IMAGE} AS build
WORKDIR /build
ARG SERVICE=auth-service
ARG SERVICE_PORT=8081
ARG SKIP_TESTS=true
RUN case "${SERVICE}:${SERVICE_PORT}" in \
      auth-service:8081|academic-service:8082|teaching-service:8083|finance-service:8084|api-gateway:8080|assistant-service:8086) ;; \
      *) echo "Unsupported SERVICE/SERVICE_PORT pair" >&2; exit 64 ;; \
    esac && case "${SKIP_TESTS}" in true|false) ;; *) exit 64 ;; esac
COPY ${SERVICE}/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline
COPY ${SERVICE}/src ./src
COPY deployment/docker/Healthcheck.java /health/Healthcheck.java
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress -DskipTests=${SKIP_TESTS} package && \
    mkdir -p /out/health && \
    set -- target/*.jar && test "$#" -eq 1 && test -f "$1" && \
    cp "$1" /out/app.jar && \
    javac --release 21 -d /out/health /health/Healthcheck.java

FROM ${JAVA_IMAGE} AS runtime
ARG SERVICE=auth-service
ARG SERVICE_PORT=8081
LABEL org.opencontainers.image.title="language-center-${SERVICE}"
WORKDIR /app
RUN groupadd --gid 10001 app && \
    useradd --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin app && \
    mkdir -p /app/data/documents && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /out/ /app/
ENV SPRING_PROFILES_ACTIVE=prod,docker \
    TZ=Asia/Ho_Chi_Minh \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=${SERVICE_PORT} \
    SERVICE_NAME=${SERVICE} \
    DOCUMENT_STORAGE=/app/data/documents \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh"
USER 10001:10001
EXPOSE ${SERVICE_PORT}
HEALTHCHECK --interval=30s --timeout=8s --start-period=90s --retries=3 \
    CMD ["java", "-cp", "/app/health", "Healthcheck"]
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
