FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy Maven build
COPY target/audittrail-0.1.0-BETA.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/audittrail/api/health || exit 1

# Run application
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
