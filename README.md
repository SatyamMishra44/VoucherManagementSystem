# Voucher Management System

Spring Boot REST API for creating users, issuing vouchers, redeeming vouchers against bills, and tracking transactions/redemption history. The project is built around simple voucher eligibility rules (date window, minimum bill, global usage cap, and one-time-per-user redemption).

## Features

- User registration and lookup
- Voucher creation, enable/disable, lookup, and eligibility listing
- Voucher redemption with validation and audit history
- Bill and transaction creation with user linkage
- MySQL persistence with JPA/Hibernate

## Tech Stack

- Java 21
- Spring Boot 3.4.2 (Web, Validation, Data JPA)
- MySQL
- Maven

## Domain Model (High Level)

- User: basic identity and contact info
- Voucher: code, discount percentage, min bill, date window, usage limits
- VoucherRedemption: per-user redemption audit + discount applied
- Transaction: pre- and post-discount amounts
- Bill: bill created for a user (no discount logic in this layer)

## Business Rules (as implemented)

- Voucher must be enabled
- Voucher must be within start and expiry dates
- Bill amount must be >= voucher minBillAmount
- Voucher has a global usage cap (maxGlobalUses)
- A user can redeem a given voucher only once
- Usage count is incremented atomically during redemption

## API Overview

Base URL: `http://localhost:8080/api/v1`

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/users` | Create user |
| GET | `/users` | List all users |

### Vouchers

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/vouchers` | Create voucher |
| PATCH | `/vouchers/{voucherId}/status?enabled=true|false` | Enable/disable voucher |
| GET | `/vouchers/{voucherId}` | Get voucher by id |
| GET | `/vouchers/code/{code}` | Get voucher by code |
| GET | `/vouchers` | List all vouchers |
| GET | `/vouchers/eligible?userId=...` | List eligible vouchers for user |
| GET | `/vouchers/validate/{code}` | Validate voucher (basic rules) |
| GET | `/vouchers/{voucherId}/redemptions` | Voucher redemption history |

### Redemptions

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/redemptions` | Redeem voucher |
| GET | `/redemptions/user/{userId}` | User redemption history |

### Bills

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/bills` | Create bill |
| GET | `/bills/{billId}` | Get bill by id |
| GET | `/bills/user/{userId}` | List bills for user |

### Transactions

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/transactions` | Create transaction |
| GET | `/transactions/{transactionId}` | Get transaction by id |
| GET | `/transactions/user/{userId}` | List transactions for user |

## Sample Requests

### Create User

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Asha",
    "lastName": "Verma",
    "password": "ChangeMe123",
    "phoneNumber": "9998887776",
    "email": "asha.verma@example.com"
  }'
```

### Create Voucher

```bash
curl -X POST http://localhost:8080/api/v1/vouchers \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "code": "SAVE10",
    "discountPercentage": 10,
    "minBillAmount": 500,
    "startDate": "2025-01-01",
    "expiryDate": "2025-12-31",
    "usageLimit": 100
  }'
```

### Redeem Voucher

```bash
curl -X POST http://localhost:8080/api/v1/redemptions \
  -H "Content-Type: application/json" \
  -d '{
    "voucherCode": "SAVE10",
    "userId": 1,
    "billAmount": 1250
  }'
```

### Get Eligible Vouchers

```bash
curl "http://localhost:8080/api/v1/vouchers/eligible?userId=1"
```

## Validation Highlights

- `UserCreateRequestDto`
  - phoneNumber: exactly 10 digits
  - email: format and allowed characters
- `VoucherCreateRequestDto`
  - code length 3-20
  - discountPercentage: (0, 100]
  - usageLimit: positive
- `VoucherRedemptionRequestDto`
  - billAmount: positive

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=VoucherMangementSystem
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/voucher_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

## Run Locally

Prereqs:

- Java 21
- Maven
- MySQL running with an empty `voucher_db` database

Commands:

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## Notes and Limitations

- Authentication/authorization is not enabled.
- Passwords are stored as plain text in `User`; use hashing before production use.
- Tests are currently disabled (no test dependencies in `pom.xml`).

## Project Structure

```
src/main/java/com/example/Voucher
  controller/    REST controllers
  dto/           Request/response DTOs
  entity/        JPA entities
  repository/    Spring Data repositories
  service/       Service interfaces
  serviceImpl/   Service implementations
```
