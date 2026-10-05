# syntax=docker/dockerfile:1

# ---- Build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle wrapper and build scripts first, so dependency resolution is cached between builds
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY build-logic build-logic
COPY app/build.gradle.kts app/
COPY common/build.gradle.kts common/
COPY user/build.gradle.kts user/
COPY chat/build.gradle.kts chat/
COPY notification/build.gradle.kts notification/
COPY club/build.gradle.kts club/
COPY match/build.gradle.kts match/
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :app:dependencies > /dev/null

COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :app:bootJar -x test

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S squadfy && adduser -S squadfy -G squadfy
COPY --from=build --chown=squadfy:squadfy /workspace/app/build/libs/squadfy.jar app.jar
USER squadfy

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
