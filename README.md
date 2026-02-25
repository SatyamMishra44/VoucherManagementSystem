# Voucher Management System

A Spring Boot backend that manages voucher templates, voucher purchases, voucher redemption, bills, and transactions with JWT authentication and role-based access control.

This README is written for beginners and maps to the current codebase.

## What This Project Does

- User registration and login with JWT access + refresh tokens
- Role-based authorization (`ADMIN`, `USER`)
- Admin voucher template management (create, enable/disable)
- User voucher purchase and redemption
- Redemption history tracking
- Bill and transaction retrieval
- Swagger/OpenAPI documentation

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Web
- Spring Security + JWT (`jjwt`)
- Spring Data Redis (refresh token cache)
- Spring Data JPA (Hibernate)
- MySQL
- Bean Validation (Jakarta Validation)
- Springdoc OpenAPI (Swagger UI)
- Maven Wrapper (`./mvnw`)

## Project Structure

```text
src/main/java/com/example/Voucher
├── config          # OpenAPI/Swagger config
├── controller      # REST endpoints
├── dto             # Request/response models
├── entity          # JPA entities
├── exception       # Global exception handling
├── repository      # Spring Data repositories
├── security        # JWT filter, auth handlers, role seeding, security config
├── service         # Service interfaces and auth/current-user services
└── serviceImpl     # Business logic implementations
```

## High-Level Flow

1. Register with `/api/v1/auth/register` (new users are assigned `USER` role).
2. Login with `/api/v1/auth/login` and receive access + refresh tokens.
3. Use `Authorization: Bearer <access-token>` for protected APIs.
4. Use `/api/v1/auth/refresh` with refresh token to issue a new access token.
5. Use `/api/v1/auth/logout` to invalidate refresh token.
6. Admin creates voucher templates.
7. Users list eligible templates, purchase vouchers, and redeem balance.
8. Users can view their vouchers and redemption history.

## Authentication and Roles

- Public endpoints:
  - `/api/v1/auth/**`
  - `/v3/api-docs/**`, `/swagger-ui/**`
- All other endpoints require JWT.
- Roles are seeded on startup by `RoleSeeder`:
  - `ADMIN`
  - `USER`

Important: Registration always assigns `USER` role. There is no API to create an admin user.

## API Base URL

`http://localhost:8080/api/v1`

## API Endpoints

### Auth

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Users

- `GET /users` (ADMIN only)

### Admin Vouchers

- `POST /admin/vouchers` (ADMIN only)
- `PATCH /admin/vouchers/{templateId}/status?enabled=true|false` (ADMIN only)

### User Vouchers

- `GET /vouchers` (USER only, list eligible templates)
- `POST /vouchers/purchase` (USER only)
- `POST /vouchers/redeem` (USER only)
- `GET /vouchers/mine` (USER only)
- `GET /vouchers/redemptions` (USER only)

### Bills

- `POST /bills` (ADMIN only)
- `GET /bills/{billId}` (ADMIN or owner USER)
- `GET /bills/user/{userId}` (ADMIN or same USER)

### Transactions

- `GET /transactions/user/{userId}` (ADMIN or same USER)

## Request Examples

### 1. Register

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

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
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
  "accessTokenExpiresInSeconds": 600,
  "refreshTokenExpiresInSeconds": 604800
}
```

### 3. Refresh Access Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-jwt>"
  }'
```

### 4. Logout (Invalidate Refresh Token)

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-jwt>"
  }'
```

### 5. Create Voucher Template (Admin)

```bash
curl -X POST http://localhost:8080/api/v1/admin/vouchers \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "FOOD100",
    "unitValue": 100,
    "startDate": "2026-02-01",
    "expiryDate": "2026-12-31"
  }'
```

### 6. Purchase Voucher (User)

```bash
curl -X POST http://localhost:8080/api/v1/vouchers/purchase \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "voucherCode": "FOOD100",
    "quantity": 3
  }'
```

### 7. Redeem Voucher (User, with bill id)

```bash
curl -X POST http://localhost:8080/api/v1/vouchers/redeem \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userVoucherId": 1,
    "billId": 10
  }'
```

### 8. Create Bill (Admin)

```bash
curl -X POST http://localhost:8080/api/v1/bills \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "totalAmount": 650
  }'
```

## Business Rules (Implemented)

- Voucher template must be enabled and date-valid to be purchased/redeemed.
- Purchased voucher starts as `ACTIVE`; becomes `INACTIVE` when balance reaches zero.
- Redemption amount is `min(bill.totalAmount, remainingBalance)`.
- `billId` is required for redemption; bill amount is always fetched from DB and a transaction record is created.
- Redemption uses pessimistic locking (`PESSIMISTIC_WRITE`) on user voucher row to avoid race conditions.

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

- `400` validation/business-rule errors
- `401` missing/invalid authentication
- `403` forbidden (role/ownership mismatch)

## Configuration

Main file: `src/main/resources/application.properties`

This project loads optional secrets from:

- `config/application-secrets.properties` (recommended for local)
- or `src/main/resources/application-secrets.properties`

Example `config/application-secrets.properties`:

```properties
DB_PASSWORD=your_mysql_password
JWT_SECRET=replace_with_at_least_32_characters_secret
```

Important env/properties:

- `DB_URL` (default: `jdbc:mysql://127.0.0.1:3306/new_voucher_db?...`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (required)
- `JWT_SECRET` (required)
- `security.jwt.access-expiration-seconds` (default: `600`)
- `security.jwt.refresh-expiration-seconds` (default: `604800`)
- `REDIS_HOST` (default: `127.0.0.1`)
- `REDIS_PORT` (default: `6379`)

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

## How to Get an Admin User (for local testing)

Because `/auth/register` creates only `USER`, you must promote a user in DB.

Example SQL (adjust values for your user):

```sql
-- find user and roles
SELECT id, email FROM users;
SELECT id, name FROM roles;

-- map user to ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com';
```

Then login again for a token containing admin authority.

## Notes for Contributors

- Tests are currently minimal (`src/test/java/...` has no active coverage).
- `spring.jpa.hibernate.ddl-auto=update` is convenient for local development, but use migrations for production.
- Keep `config/application-secrets.properties` out of version control.
