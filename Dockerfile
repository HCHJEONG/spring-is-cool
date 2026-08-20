FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN mkdir -p /app/runtime/ssh

COPY --from=builder /workspace/build/libs/*.jar /app/spring-is-cool.jar

ENV SPRING_IS_COOL_SSH_ENABLED=true
ENV SPRING_IS_COOL_SSH_HOST=0.0.0.0
ENV SPRING_IS_COOL_SSH_PORT=2222
ENV SPRING_IS_COOL_SSH_HOST_KEY_PATH=/app/runtime/ssh/hostkey.ser

EXPOSE 2222

ENTRYPOINT ["java", "-jar", "/app/spring-is-cool.jar"]
