FROM maven:3.9-eclipse-temurin-17 AS build
LABEL authors="JONAS"

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

EXPOSE 8080

COPY --from=build /target/task-manager-api-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]