# ===================================================================
# Stage 1: Build Environment
# ===================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy the Maven project descriptor
COPY pom.xml .

# Pre-fetch and cache all Maven dependencies
# This layer will only rebuild if pom.xml changes, dramatically speeding up subsequent builds
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# ===================================================================
# Stage 2: Production JRE Runtime
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

# Set default spring profile for container deployment
ENV SPRING_PROFILES_ACTIVE=docker
ENV PORT=8080

WORKDIR /app

# Create custom storage directory for judge testcases
RUN mkdir -p /algotutor/testcases

# Create a secure, non-privileged system group and user to run the application
RUN addgroup -S algotutor && adduser -S algotutor -G algotutor

# Copy the compiled JAR artifact from Stage 1
COPY --from=builder /app/target/algo-tutor-be-0.0.1-SNAPSHOT.jar app.jar

# Grant the non-root user permissions to both application and storage directories
RUN chown -R algotutor:algotutor /app /algotutor

# Use the non-root user
USER algotutor

# Expose standard Spring Boot container port
EXPOSE 8080

# Launch application with performance-optimized JVM parameters
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-jar", "app.jar"]
