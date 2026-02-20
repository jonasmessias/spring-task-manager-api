FROM ubuntu:latest AS build
LABEL authors="JONAS"

RUN apt-get update
RUN apt-get install openjdk-17-jdk -y

COPY . .

RUN apt-get install maven -y
RUN mvn clean install

EXPOSE 8080

COPY --from=build /target/task-manager-api-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]