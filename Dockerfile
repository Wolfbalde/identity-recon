FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app


COPY target/Identity_Reconciliation-0.0.1-SNAPSHOT.jar app.jar


ENTRYPOINT ["java", "-jar", "app.jar"]