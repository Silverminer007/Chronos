FROM eclipse-temurin:21
WORKDIR /app
COPY .mvn/ ./.mvn
COPY pom.xml mvnw .env ./
COPY shared/pom.xml ./shared/pom.xml
COPY cron/pom.xml ./cron/pom.xml
COPY frontend/pom.xml ./frontend/pom.xml
RUN ./mvnw org.apache.maven.plugins:maven-dependency-plugin:3.5.0:go-offline
COPY shared/ ./shared
COPY cron/ ./cron
COPY frontend/ ./frontend

EXPOSE 8080
ENTRYPOINT ["./mvnw", "-pl", "frontend", "-am", "spring-boot:run"]