# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create uploads directory with proper permissions before switching to spring user
RUN mkdir -p /app/uploads && \
    chmod 755 /app/uploads

RUN addgroup -S spring && adduser -S spring -G spring

# Change ownership of uploads directory to spring user
RUN chown -R spring:spring /app/uploads

USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


