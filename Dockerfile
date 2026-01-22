FROM ghcr.io/graalvm/native-image-community:23 AS builder
LABEL authors="silverminer"

WORKDIR /usr/src/app

COPY mvnw mvnw
COPY .mvn .mvn
RUN chmod +x mvnw

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY . .
RUN chmod +x mvnw

RUN ./mvnw -Pnative -Pproduction native:compile

FROM alpine:latest
RUN apk add gcompat
WORKDIR /usr/src/app
COPY --from=builder /usr/src/app/target/chronos .
ENTRYPOINT ["/usr/src/app/chronos"]