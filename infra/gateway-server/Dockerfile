FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/gateway-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]