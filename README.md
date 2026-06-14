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

- PostgreSQL for `auth-service` on `localhost:55432`
- PostgreSQL for `finance-service` on `localhost:55433`

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

## Troubleshooting

If Spring Boot fails with an error like `FATAL: role "auth_user" does not exist` or
`FATAL: role "finance_user" does not exist`, first make sure the app is connecting to the
container ports from this project: `55432` for `auth-service` and `55433` for `finance-service`.

If the ports are correct, the PostgreSQL container was most likely initialized earlier with a
different user and kept its old Docker volume.

Recreate the databases from scratch:

```bash
docker compose down -v
docker compose up -d
```

This removes the old PostgreSQL data volumes and lets Docker initialize the databases
again with the users from `docker-compose.yml`.
