# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cài đặt curl và các tools cần thiết
RUN apk add --no-cache curl netcat-openbsd

# Copy file JAR từ build stage
COPY --from=build /app/target/*.jar app.jar

# Tạo user non-root để chạy app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Expose port
EXPOSE 8085

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]