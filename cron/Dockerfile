FROM eclipse-temurin:21

RUN apt-get update && apt-get -y install cron

RUN ["echo", "01", "*", "*", "*", "*", "app/mvnw", "-pl", "frontend", "-am", "spring-boot:run", ">", "/etc/crontab"]

RUN ["touch", "/var/log/cron.log"]

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

ENTRYPOINT ["cron", "&&", "tail", "-f", "./logs/chronos-cron.log"]