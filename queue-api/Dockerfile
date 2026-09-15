# ---- Build stage ----
FROM gradle:8.9-jdk17 AS build
WORKDIR /app

# Copy Gradle wrapper/config first for better layer caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

# Pre-fetch dependencies (cached layer if build files don't change)
RUN ./gradlew dependencies --no-daemon || true

# Now copy source and build
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]