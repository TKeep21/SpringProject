FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY finance-service/pom.xml finance-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml
COPY finance-service/src finance-service/src

RUN mvn -q -pl finance-service -am -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/finance-service/target/finance-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
