FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/comment-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]