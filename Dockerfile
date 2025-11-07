FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# List what was created (for debugging)
RUN ls -la target/

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the SPECIFIC shaded JAR file (not the original)
# This is the 14MB file with all dependencies, not the 9KB original
COPY --from=build /app/target/discord-claim-bot-1.0.0.jar app.jar

# Run the bot
CMD ["java", "-jar", "app.jar"]