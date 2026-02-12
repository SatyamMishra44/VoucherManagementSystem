# Voucher Management System

Spring Boot REST API for voucher lifecycle, redemption, bills, and transactions with JWT authentication, role-based authorization, Swagger documentation, and MySQL persistence.

## What This Project Implements

- JWT-based stateless authentication (`/api/v1/auth/register`, `/api/v1/auth/login`)
- Role-based access control (`ADMIN`, `USER`) with method-level authorization
- Voucher administration (create, enable/disable)
- Voucher validation and eligibility filtering
- Voucher redemption flow with audit history
- Bill and transaction APIs
- OpenAPI/Swagger UI with grouped tags and ordering
- Unit and web-layer tests using JUnit 5 + Mockito + MockMvc

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Web, Spring Validation
- Spring Security (JWT + `@PreAuthorize`)
- Spring Data JPA (Hibernate)
- MySQL
- springdoc-openapi 2.8.5
- Maven Wrapper (`./mvnw`)

## High-Level Architecture

### Layers

- `controller`: HTTP endpoints + DTO mapping
- `service` / `serviceImpl`: business rules and orchestration
- `repository`: persistence contracts
- `entity`: JPA domain model
- `security`: JWT filter, user details, security handlers, RBAC config
- `config`: OpenAPI metadata
- `exception`: global exception translation

### Package Layout

```text
src/main/java/com/example/Voucher
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
├── security
├── service
└── serviceImpl
```

## Domain Model

- `User`: first/last name, email, phone, `passwordHash`, enabled flag, roles
- `Role`: role name (`ADMIN`, `USER`)
- `Voucher`: code, discount %, min bill amount, start/expiry dates, usage cap, used count, enabled status
- `VoucherRedemption`: user + voucher + transaction link, discount applied, redeemed timestamp
- `Transaction`: total amount and final amount
- `Bill`: bill amount and owner

## Security Model

### Authentication

- Public endpoints:
  - `/api/v1/auth/**`
  - Swagger docs/UI routes
- All other endpoints require a valid `Authorization: Bearer <token>` header.

### Authorization

- `ADMIN` only:
  - `POST /api/v1/users`
  - `GET /api/v1/users`
  - `POST /api/v1/vouchers`
  - `PATCH /api/v1/vouchers/{voucherId}/status`
- `ADMIN` or `USER`:
  - Voucher read/validate/eligible endpoints
  - Redemption endpoints
  - Bill endpoints
  - Transaction endpoints

### Ownership Checks

For user-scoped fetch endpoints, `CurrentUserService.assertSelfOrAdmin(...)` enforces:

- user can read own data
- admin can read any user data

## Voucher Business Rules (Current Implementation)

### Eligibility and validation

- voucher must exist
- voucher must be enabled
- current date must be between `startDate` and `expiryDate`
- global usage must not exceed `maxGlobalUses`
- bill amount must satisfy `minBillAmount` during redemption
- same user cannot redeem the same voucher more than once

### Concurrency and consistency

- DB-level uniqueness: `(voucher_id, user_id)` in `voucher_redemptions`
- Atomic increment query in `VoucherRepository.incrementUsageIfAvailable(...)`:
  - increments only when `isEnabled=true` and `usedCount < maxGlobalUses`
  - returns `0` when limit already reached

This combination prevents over-redemption under concurrent requests.

## API Documentation (Swagger)

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Tag groups are configured for logical sequence:

1. Auth
2. Users
3. Vouchers
4. Redemptions
5. Bills
6. Transactions

Related properties:

- `springdoc.swagger-ui.tagsSorter=alpha`
- `springdoc.swagger-ui.operationsSorter=alpha`

## API Summary

Base path: `http://localhost:8080/api/v1`

### Auth (`permitAll`)

- `POST /auth/register`
- `POST /auth/login`

### Users (`ADMIN`)

- `POST /users`
- `GET /users`

### Vouchers (`ADMIN` + `USER` for reads, `ADMIN` for write/admin operations)

- `POST /vouchers` (`ADMIN`)
- `PATCH /vouchers/{voucherId}/status?enabled=true|false` (`ADMIN`)
- `GET /vouchers/{voucherId}`
- `GET /vouchers/code/{code}`
- `GET /vouchers`
- `GET /vouchers/eligible?userId={id}` (optional `userId`; defaults to current user)
- `GET /vouchers/validate/{code}`
- `GET /vouchers/{voucherId}/redemptions`

### Redemptions (`ADMIN`/`USER`)

- `POST /redemptions`
- `GET /redemptions/user/{userId}` (self-or-admin)

### Bills (`ADMIN`/`USER`)

- `POST /bills`
- `GET /bills/{billId}`
- `GET /bills/user/{userId}` (self-or-admin)

### Transactions (`ADMIN`/`USER`)

- `POST /transactions`
- `GET /transactions/{transactionId}`
- `GET /transactions/user/{userId}` (self-or-admin)

## Typical Usage Flow

1. Register user via `/auth/register`
2. Login via `/auth/login` and get JWT token
3. Authorize in Swagger UI (or send bearer token in API clients)
4. Admin creates voucher(s)
5. User checks eligible vouchers
6. User redeems voucher
7. User/admin views redemption history, bills, transactions

## Example Requests

### 1) Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9998887776",
    "password": "Password@123"
  }'
```

### 2) Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Password@123"
  }'
```

### 3) Create Voucher (Admin token)

```bash
curl -X POST http://localhost:8080/api/v1/vouchers \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SAVE10",
    "discountPercentage": 10,
    "minBillAmount": 500,
    "startDate": "2026-01-01",
    "expiryDate": "2026-12-31",
    "usageLimit": 100
  }'
```

### 4) Redeem Voucher (User token)

```bash
curl -X POST http://localhost:8080/api/v1/redemptions \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "voucherCode": "SAVE10",
    "billAmount": 1250
  }'
```

### 5) Get Voucher By ID (path variable example)

```bash
curl -X GET http://localhost:8080/api/v1/vouchers/1 \
  -H "Authorization: Bearer <TOKEN>"
```

## Validation Rules

Examples of request validations implemented in DTOs:

- Auth/User:
  - valid email format
  - phone must be exactly 10 digits
  - register password length `8..72`
- Voucher:
  - code length `3..20`
  - discount `(0, 100]`
  - usage limit must be positive
- Redemption/Bill/Transaction:
  - amounts must be positive

Validation failures are returned as HTTP `400` with `ApiError`.

## Error Handling

Custom API error shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed message",
  "timestamp": "2026-02-12T00:00:00Z"
}
```

Handled centrally for:

- `IllegalArgumentException` -> `400`
- `MethodArgumentNotValidException` -> `400`
- authentication failures -> `401`
- authorization failures -> `403`

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=VoucherMangementSystem
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/voucher_db
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

security.jwt.secret=change-this-secret-to-a-32-byte-minimum-key
security.jwt.issuer=voucher-management-system
security.jwt.expiration-seconds=86400
security.roles.admin=ADMIN
security.roles.user=USER
```

## Local Setup

Prerequisites:

- Java 21
- MySQL running locally
- Database created: `voucher_db`

Commands:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Application URL:

- `http://localhost:8080`

## Testing

### Run tests

```bash
./mvnw test
```

### Clean build + tests

```bash
./mvnw clean test
```

### Where reports are generated

- Surefire reports directory: `target/surefire-reports`
- JUnit XML files: `target/surefire-reports/TEST-*.xml`
- Text summaries: `target/surefire-reports/*.txt`

These reports are suitable for sharing with mentors/interview review.

## Current Test Coverage Scope

- Service unit tests:
  - `VoucherServiceImplTest`
  - `VoucherRedemptionServiceImplTest`
  - `BillServiceImplTest`
  - `TransactionServiceImplTest`
- Controller web tests:
  - `AuthControllerTest`
  - `UserControllerTest`

## Important Notes

- Roles (`ADMIN`, `USER`) are auto-seeded at startup by `RoleSeeder`.
- Passwords are hashed with `BCryptPasswordEncoder` before persistence.
- `TransactionCreateRequestDto.billId` exists in DTO but is not currently used in `TransactionController` business flow.
- `BillService.calculateTotalAmount(...)` currently returns `0.0` placeholder.

## Future Enhancements

- Add integration tests with MySQL Testcontainers
- Add Flyway/Liquibase migrations
- Add refresh token/blacklisting strategy
- Add pagination/filtering for list endpoints
- Improve audit metadata for update operations
