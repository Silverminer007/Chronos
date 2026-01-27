FROM eclipse-temurin:25-jdk AS builder
LABEL authors="silverminer"

WORKDIR /usr/src/app

COPY . .
RUN chmod +x mvnw

RUN ./mvnw -Pproduction package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /usr/src/app
COPY --from=builder /usr/src/app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/usr/src/app/app.jar"]
