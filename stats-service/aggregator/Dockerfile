FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/aggregator-*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]