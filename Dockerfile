FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/digital-channel-app-1.0-SNAPSHOT.jar agen46-backend.jar
RUN adduser -D -H -s /sbin/nologin appuser
USER appuser
ENTRYPOINT ["java", "-jar", "agen46-backend.jar"]