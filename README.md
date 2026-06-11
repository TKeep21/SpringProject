# finance-tracker

Basic multi-module Spring Boot project for a finance tracker.

## Modules

- `auth-service`
- `finance-service`
- `report-service`

At this stage the project contains only the base structure, Spring Boot startup classes, configuration files, and actuator health endpoints. Business logic is intentionally not implemented yet.

## Requirements

- Java 17
- Maven 3.9+
- Docker and Docker Compose

## Build

```bash
mvn clean compile
```

## Start Databases

```bash
docker compose up -d
```

This starts:

- PostgreSQL for `auth-service` on `localhost:5432`
- PostgreSQL for `finance-service` on `localhost:5433`

## Run Services

Run each service in a separate terminal:

```bash
mvn -pl auth-service spring-boot:run
```

```bash
mvn -pl finance-service spring-boot:run
```

```bash
mvn -pl report-service spring-boot:run
```

## Health Checks

- `auth-service`: http://localhost:8081/actuator/health
- `finance-service`: http://localhost:8082/actuator/health
- `report-service`: http://localhost:8083/actuator/health

## Stop Databases

```bash
docker compose down
```
