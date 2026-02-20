# Project Context: Voucher Management System

## 1) Project Snapshot
- Project type: Spring Boot REST backend.
- Primary purpose: Manage voucher templates, voucher purchases, voucher redemption, billing, and transactions with JWT-based security.
- Language/runtime: Java 21, Spring Boot 3.4.2.
- Build tool: Maven Wrapper (`./mvnw`).
- Database: MySQL via Spring Data JPA/Hibernate.

## 2) Architecture
- Layered architecture:
  - `controller`: HTTP endpoints and request/response mapping.
  - `service` / `serviceImpl`: business rules and transaction orchestration.
  - `repository`: JPA data access.
  - `entity`: persistence model.
  - `security`: JWT auth filter/config, role seeding, security handlers.
  - `dto`: request/response contracts.
  - `exception`: API error handling.

- Security model:
  - Stateless JWT auth.
  - Method-level authorization using `@PreAuthorize`.
  - Roles are configured via properties and seeded at startup (`ADMIN`, `USER`).

## 3) Package Map (Current)
- Root package: `com.example.Voucher`
- Main entrypoint: `src/main/java/com/example/Voucher/VoucherManagementSystemApplication.java`
- OpenAPI config: `src/main/java/com/example/Voucher/config/OpenApiConfig.java`
- Controllers:
  - `AuthController`
  - `UserController`
  - `AdminVoucherController`
  - `UserVoucherController`
  - `BillController`
  - `TransactionController`
- Core services:
  - `AuthService`
  - `CurrentUserService`
  - `UserVoucherServiceImpl`
  - `VoucherTemplateServiceImpl`
  - `BillServiceImpl`
  - `TransactionServiceImpl`

## 4) Domain Model and Responsibilities
- `User`: account profile, credentials (`password_hash`), enabled flag, and roles.
- `Role`: authorization role (`ADMIN`, `USER`).
- `VoucherTemplate`: reusable template (code, value, validity window, enabled/disabled).
- `UserVoucher`: user-owned voucher balance, quantity, status (`ACTIVE/INACTIVE`), redemption state.
- `RedemptionHistory`: audit trail for each redemption.
- `Bill`: bill record for a user.
- `Transaction`: payable settlement record (especially when redeeming against a bill).
- `RefreshToken` (new): stores hashed refresh tokens, expiry, revoke state, and rotation chain metadata.

## 5) Key Workflows

### 5.1 Auth Flow (Current)
1. Register (`POST /api/v1/auth/register`): creates user and assigns `USER` role.
2. Login (`POST /api/v1/auth/login`):
   - Validates credentials using `AuthenticationManager`.
   - Issues access token + refresh token.
   - Stores hashed refresh token in `refresh_tokens` table.
3. Access secured APIs using access token in `Authorization: Bearer <token>`.
4. Refresh (`POST /api/v1/auth/refresh`):
   - Validates refresh JWT and token type.
   - Verifies hashed token exists in DB and is not revoked/expired.
   - Rotates token: revokes old refresh token and creates new one.
   - Returns new access + refresh token pair.
5. Logout (`POST /api/v1/auth/logout`): revokes provided refresh token.

### 5.2 Voucher Lifecycle
1. Admin creates template and controls enabled/disabled status.
2. User lists eligible templates (enabled and within valid dates).
3. User purchases voucher (quantity > 0), creating `UserVoucher` with initial balance.
4. User redeems voucher against provided bill amount or bill ID.
5. System creates `RedemptionHistory`; if bill is involved, creates `Transaction`.

### 5.3 Authorization Rules
- `ADMIN` only:
  - List users.
  - Create/update voucher templates.
  - Create bills.
- `USER` only:
  - Voucher listing/purchase/redeem/history/mine.
- `ADMIN` or owner `USER`:
  - Bill and transaction reads constrained by user ownership checks.

## 6) Data Integrity and Concurrency
- `UserVoucherServiceImpl.redeemVoucher(...)` uses pessimistic locking via repository method `findByIdAndUserIdForUpdate(...)` to prevent concurrent overspending.
- Monetary values are normalized using `BigDecimal` scale/rounding.
- Validation is performed both at DTO and service levels.

## 7) Security Implementation Details
- Security config:
  - Stateless (`SessionCreationPolicy.STATELESS`).
  - CSRF disabled for API usage.
  - Public paths: `/api/v1/auth/**`, Swagger/OpenAPI paths.
- JWT filter:
  - Reads bearer token from header.
  - Loads user and sets `SecurityContext` if token is valid.
  - Only access tokens are accepted for request authentication.
- JWT service:
  - Uses HMAC SHA-256 with secret normalization.
  - Embeds `token_type` claim (`access` or `refresh`).

## 8) API Error Contract
- Global exceptions return standardized `ApiError`:
  - `status`, `error`, `message`, `timestamp`.
- `InvalidRefreshTokenException` maps to `401 Unauthorized`.
- Validation/business errors map to `400 Bad Request` (unless handled by security entry points).

## 9) Configuration Surface
File: `src/main/resources/application.properties`
- App/server:
  - `spring.application.name`
  - `server.port`
- DB:
  - `spring.datasource.*`
  - `spring.jpa.*`
- JWT/roles:
  - `security.jwt.secret`
  - `security.jwt.issuer`
  - `security.jwt.access-expiration-seconds`
  - `security.jwt.refresh-expiration-seconds`
  - `security.roles.admin`
  - `security.roles.user`

## 10) Test Status
- Current test footprint is minimal (`src/test/java/com/example/Voucher/VoucherMangementSystemApplicationTests.java` is no-op).
- Refresh-token flow and critical service logic currently need dedicated JUnit coverage.

## 11) Known Gaps / Next Engineering Priorities
1. Add unit/integration tests for login/refresh/logout and token rotation/replay protection.
2. Add cleanup strategy for expired/revoked refresh tokens.
3. Consider moving refresh token transport to secure HttpOnly cookies for browser clients.
4. Align `README.md` auth response examples with current login response fields (now includes refresh token + separate expiries).
5. Add API-level rate limiting for auth endpoints (`/login`, `/refresh`).

## 12) Current Repository State Note
- The working tree contains active, uncommitted refresh-token related changes across auth/security classes and new token persistence files.
- This context document reflects the current code state in the workspace.
