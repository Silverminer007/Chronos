FROM eclipse-temurin:21
COPY target/*.jar cron.jar

RUN apt-get update && apt-get -y install cron

RUN ["echo", "01", "*", "*", "*", "*", "java", "-jar", "cron.jar", ">", "/etc/crontab"]

RUN ["touch", "/var/log/cron.log"]

ENTRYPOINT ["cron", "&&", "tail", "-f", "chronos-cron.log"]