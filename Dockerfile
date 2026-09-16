FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/digital-channel-app-1.0-SNAPSHOT.jar agen46-backend.jar

ENTRYPOINT ["java", "-jar", "agen46-backend.jar"]