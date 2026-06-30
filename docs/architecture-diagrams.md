# Architecture Diagrams

Ниже несколько схем, которые помогают быстро увидеть, как устроено приложение и как сервисы связаны между собой.

## Service Map

```mermaid
flowchart LR
    client["Frontend / API client"]

    auth["auth-service<br/>:8081<br/>users, login, JWT"]
    finance["finance-service<br/>:8082<br/>groups, categories, operations"]
    report["report-service<br/>:8083<br/>reports, aggregation, CSV"]

    authDb[("auth_db<br/>users")]
    financeDb[("finance_db<br/>family_groups<br/>family_members<br/>categories<br/>operations")]

    client -->|"POST /api/v1/auth/register<br/>POST /api/v1/auth/login"| auth
    auth -->|"issues JWT"| client

    client -->|"Authorization: Bearer JWT<br/>/api/v1/groups<br/>/api/v1/categories<br/>/api/v1/operations"| finance
    client -->|"Authorization: Bearer JWT<br/>/api/v1/reports/*"| report

    finance -->|"GET /internal/users/{id}<br/>Authorization + X-Internal-Token"| auth
    report -->|"GET /internal/operations<br/>Authorization + X-Internal-Token"| finance

    auth --> authDb
    finance --> financeDb
```

## Report Request Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant ReportController as report-service<br/>ReportController
    participant ReportService as report-service<br/>ReportService
    participant FinanceClient as report-service<br/>FinanceOperationsClient
    participant FinanceInternal as finance-service<br/>InternalOperationController
    participant OperationService as finance-service<br/>OperationService
    participant FinanceDb as finance_db

    User->>ReportController: GET /api/v1/reports/summary?from=...&to=...<br/>Authorization: Bearer JWT
    ReportController->>ReportService: getSummaryReport(request, authorizationHeader)
    ReportService->>ReportService: validatePeriod(request)
    ReportService->>FinanceClient: getOperations(request, authorizationHeader)
    FinanceClient->>FinanceInternal: GET /internal/operations?from=...&to=...<br/>Authorization: Bearer JWT<br/>X-Internal-Token
    FinanceInternal->>FinanceInternal: requireValidToken(X-Internal-Token)
    FinanceInternal->>OperationService: getOperationsForReports(request)
    OperationService->>OperationService: get current user from JWT
    OperationService->>OperationService: check group membership and filters
    OperationService->>FinanceDb: query visible operations
    FinanceDb-->>OperationService: operations
    OperationService-->>FinanceInternal: InternalOperationReportItem list
    FinanceInternal-->>FinanceClient: operations JSON
    FinanceClient-->>ReportService: OperationReportItem list
    ReportService->>ReportService: sum income, expense, balance
    ReportService-->>ReportController: ReportSummaryResponse
    ReportController-->>User: JSON report
```

## Finance Data Model

```mermaid
erDiagram
    FAMILY_GROUPS {
        uuid id PK
        string name
        text description
        uuid owner_user_id
        instant created_at
        instant updated_at
    }

    FAMILY_MEMBERS {
        uuid id PK
        uuid group_id
        uuid user_id
        enum role "OWNER, ADMIN, MEMBER"
        instant joined_at
    }

    CATEGORIES {
        uuid id PK
        string name
        enum type "INCOME, EXPENSE"
        uuid owner_user_id
        uuid group_id
        boolean is_default
        instant created_at
    }

    OPERATIONS {
        uuid id PK
        uuid user_id
        uuid group_id
        uuid category_id
        enum type "INCOME, EXPENSE"
        decimal amount
        string currency
        date operation_date
        text description
        instant created_at
        instant updated_at
    }

    FAMILY_GROUPS ||--o{ FAMILY_MEMBERS : has
    FAMILY_GROUPS ||--o{ CATEGORIES : owns_group_categories
    FAMILY_GROUPS ||--o{ OPERATIONS : contains_group_operations
    CATEGORIES ||--o{ OPERATIONS : classifies
```

## Operation Visibility Rule

```mermaid
flowchart TD
    request["GET /api/v1/operations"]
    hasGroup{"groupId passed?"}

    allVisible["Find all groups where current user is a member"]
    accessAll["DB condition:<br/>(user_id = currentUser AND group_id IS NULL)<br/>OR group_id IN userGroupIds"]

    checkGroup["Check group exists"]
    checkMember["Check current user is member of groupId"]
    accessGroup["DB condition:<br/>group_id = groupId"]

    filters["Add optional filters:<br/>fromDate, toDate, type, categoryId, userId"]
    result["Return paged OperationResponse list"]

    request --> hasGroup
    hasGroup -- "No" --> allVisible --> accessAll --> filters
    hasGroup -- "Yes" --> checkGroup --> checkMember --> accessGroup --> filters
    filters --> result
```
