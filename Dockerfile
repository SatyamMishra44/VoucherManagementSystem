# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Leverage Maven wrapper and local repo cache inside the image
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies to improve rebuild speed
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source and build
COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user for runtime safety
RUN useradd -r -u 10001 appuser

COPY --from=build /workspace/target/*.jar /app/app.jar

# 🔥 FIX HERE
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

USER appuser
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
