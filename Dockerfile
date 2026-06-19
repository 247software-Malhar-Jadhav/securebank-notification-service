# =============================================================================
# Multi-stage Dockerfile for securebank-notification-service.
# Stage 1 builds the fat jar; stage 2 ships a slim JRE runtime image.
# =============================================================================

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first and pre-fetch dependencies so this layer is cached as long as
# the POM is unchanged (faster rebuilds when only source changes).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy sources and build the runnable jar (skip tests in the image build; CI runs them).
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as a non-root user for safety.
RUN useradd --system --uid 10001 appuser
USER appuser

# Copy only the built jar from the build stage.
COPY --from=build /build/target/securebank-notification-service-*.jar app.jar

# HTTP / actuator port per platform spec.
EXPOSE 8085

# Default to the docker profile so in-network hostnames (postgres/kafka/rabbitmq) apply.
ENV SPRING_PROFILES_ACTIVE=docker

# Virtual threads + container-aware memory are on by default in JRE 21.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
