FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY finance-service/pom.xml finance-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml

RUN mvn -q -pl auth-service -am -DskipTests dependency:go-offline

COPY auth-service/src auth-service/src

RUN mvn -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
