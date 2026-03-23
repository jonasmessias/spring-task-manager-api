# ── Stage 1: Build ───────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
LABEL authors="JONAS"

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ── Stage 2: Run ────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Security: run as non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/target/task-manager-api-1.0.0.jar app.jar

RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

# JVM tuning for containers (respects container memory limits)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]