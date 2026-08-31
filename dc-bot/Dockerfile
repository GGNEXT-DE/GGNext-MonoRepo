# --- Build Stage ---
FROM gradle:jdk25-alpine AS build

WORKDIR /app

COPY . .
RUN gradle :dc-bot:build --no-daemon -x test

# --- Runtime Stage ---
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S botuser && adduser -S botuser -G botuser

COPY --from=build /app/dc-bot/build/libs/*-all.jar app.jar

USER botuser

CMD ["java", "-jar", "app.jar"]