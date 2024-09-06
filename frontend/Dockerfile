FROM eclipse-temurin:21
COPY target/*.jar frontend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "frontend.jar"]