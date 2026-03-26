# Build stage
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY . .

RUN ./gradlew clean build -x test

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
