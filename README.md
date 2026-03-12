# Voucher Management System

A multi-tenant Spring Boot backend that manages voucher templates, voucher purchases, voucher redemption, bills, transactions, tenant onboarding, and PDF report delivery with JWT authentication and role-based access control.

This README is written for beginners and maps to the current codebase.

## What This Project Does

- Multi-tenant voucher platform with SYSTEM_INDIVIDUAL and ORGANIZATION tenants
- JWT access + refresh tokens with Redis-backed refresh token storage
- Platform admin management of tenants and voucher templates
- Tenant admin purchase and distribution of voucher inventory to organization users
- User voucher purchase, redemption, and redemption history
- Bills and transactions with admin filtering APIs
- Tenant onboarding requests with platform approval workflow
- PDF report generation and email delivery
- Swagger/OpenAPI documentation

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Web
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data Redis (refresh token cache)
- Spring Data JPA (Hibernate)
- MySQL
- Flyway migrations
- Bean Validation (Jakarta Validation)
- Springdoc OpenAPI (Swagger UI)
- Spring Mail (SMTP)
- OpenHTMLtoPDF (PDF rendering)
- Maven Wrapper (./mvnw)

## Project Structure

```text
src/main/java/com/example/Voucher
├── config          # Async + OpenAPI config
├── controller      # REST endpoints
├── dto             # Request/response models
├── entity          # JPA entities
├── exception       # Global exception handling
├── platform        # Platform tenant management domain
├── report          # Report rendering + email
├── repository      # Spring Data repositories
├── security        # JWT, auth handlers, role seeding, security config
├── service         # Service interfaces and core services
├── serviceImpl     # Business logic implementations
└── tenant          # Tenant context and resolver
```

## Multi-Tenancy

- Tenant context is resolved from `X-Tenant-Code` or from the authenticated user token.
- If no header is provided, the system defaults to `SYSTEM_INDIVIDUAL` tenant.
- For authenticated requests, if `X-Tenant-Code` is present it must match the token tenant or the request is rejected (403).
- For registration and login, set `X-Tenant-Code` when working with organization tenants.

## High-Level Flows

System individual user flow:

1. Register with `/api/v1/auth/register` (defaults to SYSTEM_INDIVIDUAL unless `X-Tenant-Code` is provided).
2. Login with `/api/v1/auth/login` and receive access + refresh tokens.
3. Use `Authorization: Bearer <access-token>` for protected APIs.
4. Admin creates voucher templates in the SYSTEM_INDIVIDUAL tenant catalog.
5. Users list eligible templates, purchase vouchers, and redeem balance.

Organization tenant flow:

1. Organization submits onboarding request at `/api/v1/tenant-onboarding/requests`.
2. Platform admin approves request at `/api/v1/platform/onboarding/requests/{id}/decision`.
3. Platform admin or onboarding approval creates a TENANT_ADMIN for the organization.
4. Tenant admin buys voucher stock from platform catalog and distributes to org users.
5. Tenant admin or users generate reports as needed.

## Authentication and Roles

Public endpoints:

- `/api/v1/auth/**`
- `/api/v1/tenant-onboarding/**`
- `/v3/api-docs/**`, `/swagger-ui/**`

Roles (seeded on startup):

- `PLATFORM_ADMIN`
- `TENANT_ADMIN`
- `USER`

Registration creates a `USER` in the current tenant context.
There is no API to create a `PLATFORM_ADMIN`. Use DB role assignment for local testing.

## API Base URL

`http://localhost:8080/api/v1`

If you need tenant routing on public endpoints (register/login), add `X-Tenant-Code: <TENANT_CODE>`.

## API Endpoints

### Auth

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Users

- `GET /users` (PLATFORM_ADMIN, TENANT_ADMIN) with filters: `firstName`, `lastName`, `email`, `phoneNumber`, `enabled`

### Platform Voucher Templates

- `POST /admin/vouchers` (PLATFORM_ADMIN)
- `PATCH /admin/vouchers/{templateId}/status?enabled=true|false` (PLATFORM_ADMIN)

### User Vouchers

- `GET /vouchers` (USER, TENANT_ADMIN)
- `POST /vouchers/purchase` (USER)
- `POST /vouchers/redeem` (USER)
- `GET /vouchers/mine` (USER)
- `GET /vouchers/redemptions` (USER)
- `GET /vouchers/admin/issued` (PLATFORM_ADMIN, TENANT_ADMIN) with filters for assigned user, amount range, redemption state, date range, status

### Bills

- `POST /bills` (PLATFORM_ADMIN, TENANT_ADMIN)
- `GET /bills/{billId}` (admin or owner USER)
- `GET /bills/user/{userId}` (admin or same USER)

### Transactions

- `GET /transactions/user/{userId}` (admin or same USER)
- `GET /transactions` (PLATFORM_ADMIN, TENANT_ADMIN) with filters: userId, amount ranges, fromTime, toTime

### Tenant Vouchers

- `POST /tenant/vouchers/purchase` (TENANT_ADMIN)
- `POST /tenant/vouchers/distribute` (TENANT_ADMIN)
- `GET /tenant/vouchers/inventory` (TENANT_ADMIN)
- `POST /tenant/vouchers/requests` (TENANT_ADMIN)

### Tenant Onboarding

- `POST /tenant-onboarding/requests` (public)

### Platform Onboarding Review

- `GET /platform/onboarding/requests` (PLATFORM_ADMIN) optional `status` filter
- `GET /platform/onboarding/requests/{requestId}` (PLATFORM_ADMIN)
- `PATCH /platform/onboarding/requests/{requestId}/decision` (PLATFORM_ADMIN)

### Platform Tenants

- `GET /platform/tenants` (PLATFORM_ADMIN)
- `PATCH /platform/tenants/{tenantId}/status` (PLATFORM_ADMIN)
- `POST /platform/tenants/{tenantId}/tenant-admins` (PLATFORM_ADMIN)
- `GET /platform/tenants/{tenantId}/audit-logs` (PLATFORM_ADMIN)

### Reports

- `POST /reports/me/email` (USER, SYSTEM_INDIVIDUAL only)
- `POST /reports/tenant/email` (TENANT_ADMIN)
- `GET /reports/{reportJobId}` (PLATFORM_ADMIN)

## Request Examples

### Register (system individual by default)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "password": "Password@123"
  }'
```

### Register for an organization tenant

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: ACME" \
  -d '{
    "firstName": "Jane",
    "lastName": "Admin",
    "email": "jane@acme.com",
    "phoneNumber": "9876543211",
    "password": "Password@123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: SYSTEM_INDIVIDUAL" \
  -d '{
    "email": "john@example.com",
    "password": "Password@123"
  }'
```

Login response:

```json
{
  "accessToken": "<access-jwt>",
  "refreshToken": "<refresh-jwt>",
  "tokenType": "Bearer",
  "accessTokenExpiresInSeconds": 6000,
  "refreshTokenExpiresInSeconds": 604800
}
```

### Create Voucher Template (Platform Admin)

```bash
curl -X POST http://localhost:8080/api/v1/admin/vouchers \
  -H "Authorization: Bearer <PLATFORM_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "FOOD100",
    "unitValue": 100,
    "startDate": "2026-02-01",
    "expiryDate": "2026-12-31"
  }'
```

### Tenant Admin: Purchase Voucher Stock

```bash
curl -X POST http://localhost:8080/api/v1/tenant/vouchers/purchase \
  -H "Authorization: Bearer <TENANT_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "voucherCode": "FOOD100",
    "quantity": 50
  }'
```

### User: Redeem Voucher

```bash
curl -X POST http://localhost:8080/api/v1/vouchers/redeem \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userVoucherId": 1,
    "billId": 10
  }'
```

### User: Request Own Report by Email

```bash
curl -X POST http://localhost:8080/api/v1/reports/me/email \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "fromDate": "2026-01-01",
    "toDate": "2026-01-31",
    "recipientEmail": "me@example.com"
  }'
```

## Business Rules (Implemented)

- Voucher template must be enabled and date-valid to be purchased or redeemed.
- Purchased voucher starts as `ACTIVE` and becomes `INACTIVE` when balance reaches zero.
- Redemption amount is `min(bill.totalAmount, remainingBalance)` and creates a transaction.
- Redemption uses pessimistic locking (`PESSIMISTIC_WRITE`) on the user voucher row.
- Report date range cannot exceed 180 days.
- Tenant context is enforced for users, vouchers, bills, and transactions.

## Validation Rules

- Email must be valid format.
- Phone number must be exactly 10 digits.
- Register password length: 8 to 72.
- Monetary and quantity values must be positive where required.

## Error Response Format

The API returns a consistent error shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed message",
  "timestamp": "2026-02-18T12:00:00Z"
}
```

Common cases:

- `400` validation or business-rule errors
- `401` missing or invalid authentication
- `403` forbidden (role or ownership mismatch)

## Configuration

Main file: `src/main/resources/application.properties`

This project loads optional secrets from:

- `config/application-secrets.properties` (recommended for local)
- `src/main/resources/application-secrets.properties`

Important env/properties:

- `DB_URL` (default: `jdbc:mysql://127.0.0.1:3306/new_voucher_db?...`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (required)
- `JWT_SECRET` (required)
- `JWT_ACCESS_EXPIRATION_SECONDS` (default: `6000`)
- `JWT_REFRESH_EXPIRATION_SECONDS` (default: `604800`)
- `security.roles.platform-admin` (default: `PLATFORM_ADMIN`)
- `security.roles.tenant-admin` (default: `TENANT_ADMIN`)
- `security.roles.user` (default: `USER`)
- `REDIS_HOST` (default: `127.0.0.1`)
- `REDIS_PORT` (default: `6379`)
- `REPORT_STORAGE_PATH` (default: `./generated-reports`)
- `REPORT_EMAIL_FROM` (default: `no-reply@voucher.local`)
- `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password` (required for report emails)

## Local Setup

Prerequisites:

- Java 21
- MySQL running locally
- Redis running locally

Steps:

1. Create `config/application-secrets.properties` with `DB_PASSWORD` and `JWT_SECRET`.
2. Start MySQL.
3. Start Redis.
4. Run the app:

```bash
./mvnw spring-boot:run
```

App URLs:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Admin Setup (Local Testing)

To create a PLATFORM_ADMIN user, promote a user in DB.

Example SQL:

```sql
-- find user and roles
SELECT id, email FROM users;
SELECT id, name FROM roles;

-- map user to PLATFORM_ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'PLATFORM_ADMIN'
WHERE u.email = 'admin@example.com';
```

To create a TENANT_ADMIN, use platform onboarding approval or the platform admin API.

## Notes for Contributors

- Flyway runs on startup with `spring.jpa.hibernate.ddl-auto=validate`.
- Report emails require SMTP config, otherwise report jobs will fail on email step.
- Keep `config/application-secrets.properties` out of version control.
