# =========================================================================
# STAGE 1: Build the Spring Boot Application
# =========================================================================
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
COPY mvnw.cmd .

# Ensure mvnw has execute permissions and package the application
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# =========================================================================
# STAGE 2: Create the Lightweight Runtime Image
# =========================================================================
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the packaged jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the API port
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
