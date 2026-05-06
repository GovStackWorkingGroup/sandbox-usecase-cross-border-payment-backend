# ---- Build stage ----
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle clean build -x test

# ---- Runtime stage ----
FROM gradle:8.14-jdk21
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]