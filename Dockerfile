FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

ENV SPRING_PROFILES_ACTIVE=docker
ENV PORT=8080

WORKDIR /app

RUN mkdir -p /algotutor/testcases
RUN addgroup -S algotutor && adduser -S algotutor -G algotutor

COPY --from=builder /app/target/algo-tutor-be-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R algotutor:algotutor /app /algotutor

USER algotutor

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-jar", "app.jar"]