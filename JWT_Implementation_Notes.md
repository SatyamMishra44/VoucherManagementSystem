# JWT Implementation Notes

## Big Picture: How JWT Authentication Works
JWT turns login into a signed token that the client carries on every request. The server stays stateless: it validates the token, loads the user, and authorizes based on roles. No server-side session storage.

## Overall Request Flow (Login → Token → Request → Validation)
1) User logs in with email/password.
2) Server verifies credentials and returns a signed JWT.
3) Client sends JWT in `Authorization: Bearer <token>`.
4) Server validates token, loads user details, and sets authentication context.
5) Controllers and method-level rules enforce role-based access.

## File-by-File Breakdown

### `src/main/java/com/example/Voucher/security/SecurityConfig.java`
What we did
- Created the security configuration, enabled method security, registered JWT filter, and defined stateless session policy.
- Exposed `PasswordEncoder` and `AuthenticationManager` beans.

Why we implemented it
- Without explicit config, Spring Security blocks all endpoints or applies defaults (session/basic login) that conflict with JWT.
- This file is the central policy: which requests are public, which are authenticated, and how authentication is built.

What happens if skipped
- All endpoints are locked with default login (not JWT). Token validation never runs.

How it connects
- Authentication: wires JWT filter into the chain.
- Role-based authorization: enables method security (`@PreAuthorize`).
- Security best practices: stateless session, disables form login/basic.

---

### `src/main/java/com/example/Voucher/security/JwtProperties.java`
What we did
- Bound JWT settings (secret, issuer, expiration) from config.

Why we implemented it
- Secrets and timing must be externalized for real-world deployment.

What happens if skipped
- You hardcode secrets in code, which is unsafe and inflexible.

How it connects
- Authentication: token signing/verification uses these values.
- Security best practices: config-driven secrets and rotation.

---

### `src/main/java/com/example/Voucher/security/JwtService.java`
What we did
- Built token generation and validation using JJWT.
- Extracted subject (email) and validated expiration.

Why we implemented it
- This is the “token provider”: one place to issue/verify tokens.

What happens if skipped
- Token creation and validation become scattered, inconsistent, and error-prone.

How it connects
- Authentication: issues tokens on login and validates on request.
- Security best practices: single responsibility and centralized validation.

---

### `src/main/java/com/example/Voucher/security/JwtAuthenticationFilter.java`
What we did
- Added a `OncePerRequestFilter` that reads the Authorization header, validates the token, and sets authentication in the SecurityContext.

Why we implemented it
- This is how every request becomes “authenticated” without sessions.

What happens if skipped
- The token is never checked, so your app treats every request as anonymous.

How it connects
- Authentication: establishes identity for each request.
- Role-based authorization: required for role checks to work.

---

### `src/main/java/com/example/Voucher/security/CustomUserDetailsService.java`
What we did
- Loaded users by email and mapped their roles to Spring Security authorities.

Why we implemented it
- Spring Security needs a canonical way to load users and their roles.

What happens if skipped
- Authentication fails because Spring can’t build `UserDetails`.

How it connects
- Authentication: validates credentials.
- Role-based authorization: exposes roles to security context.

---

### `src/main/java/com/example/Voucher/security/RoleProperties.java` and `RoleSeeder.java`
What we did
- Externalized role names (`ADMIN`, `USER`) and seeded them into DB at startup.

Why we implemented it
- Avoid hardcoding roles; DB-backed roles allow evolution and consistency.

What happens if skipped
- Roles may be missing or mismatched, causing authorization failures.

How it connects
- Role-based authorization: roles exist and are consistent with checks.
- Security best practices: avoid magic strings in code.

---

### `src/main/java/com/example/Voucher/service/AuthService.java`
What we did
- Implemented register/login logic.
- On register: create user, assign USER role, return JWT.
- On login: authenticate and return JWT.

Why we implemented it
- This is the boundary between credentials and token issuance.

What happens if skipped
- No authentication flow, no tokens, no secure API access.

How it connects
- Authentication: core login/register flow.
- Role-based authorization: assigns USER role on register.

---

### `src/main/java/com/example/Voucher/controller/AuthController.java`
What we did
- Exposed `/api/v1/auth/register` and `/api/v1/auth/login` endpoints.

Why we implemented it
- Provides the entry points for clients to get a token.

What happens if skipped
- Clients cannot authenticate or obtain tokens.

How it connects
- Authentication: public endpoints for token issuance.

---

### `src/main/java/com/example/Voucher/entity/User.java` and `Role.java`
What we did
- Added `Role` entity and `User.roles` many-to-many mapping.
- Renamed password field to `password_hash` and introduced `enabled`.

Why we implemented it
- Roles must be persisted for RBAC.
- Passwords must be hashed for security.

What happens if skipped
- Cannot enforce role-based access, and passwords are insecure.

How it connects
- Authentication: user identity store with hashed credentials.
- Role-based authorization: persisted roles.

---

### `src/main/java/com/example/Voucher/serviceImpl/UserServiceImpl.java`
What we did
- Encoded passwords using BCrypt on user creation.

Why we implemented it
- Hashing is mandatory for production security.

What happens if skipped
- Plaintext passwords = severe breach risk.

How it connects
- Security best practices: protects credentials at rest.

---

### `src/main/java/com/example/Voucher/security/RestAuthenticationEntryPoint.java` and `RestAccessDeniedHandler.java`
What we did
- Ensured consistent JSON responses for 401 and 403.

Why we implemented it
- Default security responses are not API-friendly and can leak details.

What happens if skipped
- Inconsistent errors and poor client experience.

How it connects
- Security best practices: predictable error handling.

---

### `src/main/java/com/example/Voucher/exception/GlobalExceptionHandler.java` and `ApiError.java`
What we did
- Standardized API error responses for validation and bad input.

Why we implemented it
- Makes errors stable and debuggable.

What happens if skipped
- Random error shapes, harder client handling.

How it connects
- Security best practices: avoids leaking stack traces.

---

### `src/main/resources/application.properties`
What we did
- Added JWT secret, issuer, expiration, and role names.

Why we implemented it
- Externalized configuration for deployment flexibility.

What happens if skipped
- Secrets end up hardcoded, insecure, and unmanageable.

How it connects
- Security best practices: configuration management.

## Security Flow Diagram (explained in text form)
1) Client logs in → `AuthController` → `AuthService` → `AuthenticationManager`.
2) `CustomUserDetailsService` loads user + roles.
3) `JwtService` signs a JWT → client stores it.
4) Client sends `Authorization: Bearer <token>`.
5) `JwtAuthenticationFilter` validates token and sets `SecurityContext`.
6) `@PreAuthorize` checks roles for each endpoint.
7) If unauthorized → `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` returns JSON error.

## Mental Model (Component-by-Component)

JWT Filter
- When a request comes in, the filter checks the Authorization header.
- If a token exists, it validates it and loads the user details.
- It then sets Authentication in the SecurityContext so controllers see the user as logged in.

JWT Service
- When login succeeds, it signs a token using the secret and expiry.
- When a request comes in, it verifies signature and expiration.

Security Config
- Spring Security reads this config before any controller runs.
- It wires the JWT filter and enforces “authenticated unless public.”
- It enables method-level role checks (`@PreAuthorize`).

CustomUserDetailsService
- When Spring needs to authenticate or authorize, it uses this to load user + roles.
- This is the bridge between DB and security framework.

AuthService
- Handles registration and login flows.
- Returns JWTs so the client can authenticate future requests.

RoleSeeder
- On startup, ensures roles exist in the DB.
- Prevents missing-role errors later.

## Common Mistakes & Why They Happen
- Hardcoding role names: leads to mismatches and hard-to-change rules.
- Storing plaintext passwords: security breach risk and compliance violation.
- Forgetting stateless config: sessions and JWT conflict.
- Skipping the JWT filter: tokens are ignored, requests are anonymous.
- Leaving endpoints open: security config defaults can block or expose APIs.

## How to Rebuild JWT From Scratch (Step Checklist)
1) Add Spring Security + JJWT dependencies.
2) Create `Role` and `User` with a many-to-many mapping.
3) Add `UserDetailsService` to load users by email and map roles.
4) Add BCrypt `PasswordEncoder` and hash passwords on create.
5) Add `JwtProperties` for secret/issuer/expiry.
6) Add `JwtService` for token creation and validation.
7) Add `JwtAuthenticationFilter` and wire it in `SecurityConfig`.
8) Expose `/auth/register` and `/auth/login` endpoints.
9) Enforce roles using `@PreAuthorize`.
10) Add consistent error handlers for 401/403 and validation errors.

## Interview Explanation Version (short explanation)
“I built JWT auth by adding Spring Security with a stateless filter. Login authenticates via AuthenticationManager, then issues a signed JWT. Each request passes through a JWT filter that validates the token and loads user authorities from the DB using UserDetailsService. Controllers enforce roles via @PreAuthorize. Roles are seeded in the DB and passwords are stored hashed with BCrypt. Errors are standardized with JSON handlers for 401/403 and validation.”
