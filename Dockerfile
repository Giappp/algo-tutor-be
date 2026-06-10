FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

ENV SPRING_PROFILES_ACTIVE=docker
ENV PORT=8080

WORKDIR /app

RUN mkdir -p /algotutor/testcases \
    && addgroup -S algotutor \
    && adduser -S algotutor -G algotutor

COPY --from=builder /app/target/algo-tutor-be-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R algotutor:algotutor /app /algotutor

USER algotutor

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-jar", "app.jar"]