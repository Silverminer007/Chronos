FROM eclipse-temurin:21

RUN apt-get update && apt-get -y install cron

RUN ["echo", "01", "*", "*", "*", "*", "java", "-jar", "chronos-cron.jar", ">", "/etc/crontab"]

RUN ["touch", "/var/log/cron.log"]

COPY ./target/*.jar ./chronos-cron.jar

ENTRYPOINT ["cron", "&&", "tail", "-f", "chronos-cron.log"]