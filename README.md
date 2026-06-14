# finance-tracker

Backend-система учета личных и семейных финансов на Spring Boot. Проект разбит на три сервиса:

- `auth-service` - регистрация, вход, выпуск JWT и internal API пользователей.
- `finance-service` - группы, участники, категории, доходы и расходы.
- `report-service` - отчеты, агрегация операций и CSV-выгрузка.

Сервисы общаются синхронно по REST. Пользовательский доступ защищен JWT, internal REST-вызовы дополнительно защищены header `X-Internal-Token`.

## Стек

- Java 17
- Spring Boot 3.3.6
- Spring MVC
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- springdoc OpenAPI / Swagger UI
- Docker Compose

## Запуск

Собрать проект локально:

```bash
mvn clean compile
```

Поднять всю систему через Docker Compose:

```bash
docker compose up --build
```

Остановить контейнеры:

```bash
docker compose down
```

Полностью пересоздать БД и данные:

```bash
docker compose down -v
docker compose up --build
```

## URLs

Health checks:

- `auth-service`: http://localhost:8081/actuator/health
- `finance-service`: http://localhost:8082/actuator/health
- `report-service`: http://localhost:8083/actuator/health

Swagger UI:

- `auth-service`: http://localhost:8081/swagger-ui/index.html
- `finance-service`: http://localhost:8082/swagger-ui/index.html
- `report-service`: http://localhost:8083/swagger-ui/index.html

PostgreSQL:

- `auth_db`: `localhost:55432`
- `finance_db`: `localhost:55433`

## Environment

Основные переменные окружения:

| Variable | Default | Used by |
| --- | --- | --- |
| `JWT_SECRET` | `change-me-change-me-change-me-change-me-1234567890` | all services |
| `JWT_EXPIRATION` | `3600000` | auth-service |
| `INTERNAL_SERVICE_TOKEN` | `change-me-internal-service-token` | all services |
| `AUTH_DB_URL` | `jdbc:postgresql://localhost:55432/auth_db` | auth-service |
| `AUTH_DB_USERNAME` | `auth_user` | auth-service |
| `AUTH_DB_PASSWORD` | `auth_password` | auth-service |
| `FINANCE_DB_URL` | `jdbc:postgresql://localhost:55433/finance_db` | finance-service |
| `FINANCE_DB_USERNAME` | `finance_user` | finance-service |
| `FINANCE_DB_PASSWORD` | `finance_password` | finance-service |
| `AUTH_SERVICE_BASE_URL` | `http://localhost:8081` | finance-service |
| `FINANCE_SERVICE_BASE_URL` | `http://localhost:8082` | report-service |

В Docker Compose сервисные URL переопределены на имена контейнеров: `http://auth-service:8081` и `http://finance-service:8082`.

## Основные API

Auth:

- `POST /api/v1/auth/register` - регистрация
- `POST /api/v1/auth/login` - вход
- `GET /api/v1/auth/me` - текущий пользователь

Groups:

- `POST /api/v1/groups` - создать группу
- `GET /api/v1/groups` - мои группы
- `GET /api/v1/groups/{groupId}` - группа по id
- `PUT /api/v1/groups/{groupId}` - обновить группу
- `DELETE /api/v1/groups/{groupId}` - удалить группу
- `POST /api/v1/groups/{groupId}/members` - добавить участника
- `DELETE /api/v1/groups/{groupId}/members/{userId}` - удалить участника
- `PATCH /api/v1/groups/{groupId}/members/{userId}/role` - сменить роль участника
- `POST /api/v1/groups/{groupId}/transfer-owner` - передать владение группой

Categories:

- `POST /api/v1/categories` - создать личную или групповую категорию
- `GET /api/v1/categories?type=EXPENSE&groupId=...` - получить доступные категории
- `PUT /api/v1/categories/{categoryId}` - обновить категорию
- `DELETE /api/v1/categories/{categoryId}` - удалить категорию

Operations:

- `POST /api/v1/operations` - создать доход или расход
- `GET /api/v1/operations` - операции с фильтрами и пагинацией
- `GET /api/v1/operations/{operationId}` - операция по id
- `PATCH /api/v1/operations/{operationId}` - частично обновить операцию
- `DELETE /api/v1/operations/{operationId}` - удалить операцию

Reports:

- `GET /api/v1/reports/summary?from=2026-06-01&to=2026-06-30&currency=USD`
- `GET /api/v1/reports/by-category?from=2026-06-01&to=2026-06-30&currency=USD`
- `GET /api/v1/reports/by-member?from=2026-06-01&to=2026-06-30&groupId=...&currency=USD`
- `GET /api/v1/reports/export?from=2026-06-01&to=2026-06-30&format=csv`

Для агрегированных отчетов (`summary`, `by-category`, `by-member`) операции должны быть в одной валюте. Можно передать `currency=USD`, `currency=RUB` и т.д. Если валют несколько и фильтр не указан, API вернет ошибку вместо математически неверной суммы. CSV-экспорт может содержать операции в разных валютах, потому что он не суммирует значения.

## Быстрый сценарий

Зарегистрировать пользователя:

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "alice@example.com",
    "password": "password123",
    "firstName": "Alice",
    "lastName": "Smith"
  }'
```

Сохранить токен:

```bash
TOKEN='<accessToken from register/login response>'
```

Создать группу:

```bash
curl -s -X POST http://localhost:8082/api/v1/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Family Budget",
    "description": "Shared family finances"
  }'
```

Создать личную категорию:

```bash
curl -s -X POST http://localhost:8082/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Coffee",
    "type": "EXPENSE"
  }'
```

Создать операцию с одной из default-категорий:

```bash
curl -s -X POST http://localhost:8082/api/v1/operations \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "categoryId": "10000000-0000-0000-0000-000000000001",
    "type": "EXPENSE",
    "amount": 42.50,
    "currency": "USD",
    "operationDate": "2026-06-14",
    "description": "Groceries"
  }'
```

Получить отчет:

```bash
curl -s "http://localhost:8083/api/v1/reports/summary?from=2026-06-01&to=2026-06-30&currency=USD" \
  -H "Authorization: Bearer $TOKEN"
```

## Access Rules

- Пользователь видит свои личные операции.
- Пользователь видит операции групп, где он состоит.
- Управлять составом группы могут `OWNER` и `ADMIN`.
- Удалять группу, менять роли и передавать владельца может `OWNER`.
- Default-категории нельзя изменять или удалять.
- Личные категории доступны только владельцу.
- Групповые категории могут изменять `OWNER` и `ADMIN`.
- Internal endpoints `/internal/**` требуют `X-Internal-Token` и не предназначены для публичного API.
