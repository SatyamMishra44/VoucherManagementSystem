# Voucher Management System - Security, Authentication, Authorization, and Role-Based Access Guide

## 1) Purpose of this document
This guide explains the full security flow in your project so you can confidently answer mentor questions.
It covers:
- What each security-related file does
- What each important method does
- Why each annotation/keyword exists
- End-to-end request flow reasoning
- Typical mentor questions and strong answers

---

## 2) High-level architecture (mental model)
Think in 6 layers:
1. **Config layer**: `SecurityConfig` defines security rules and filter chain.
2. **Token layer**: `JwtService` creates and validates JWT.
3. **Filter layer**: `JwtAuthenticationFilter` reads `Authorization` header and sets authenticated user.
4. **Identity layer**: `CustomUserDetailsService` loads user + roles from DB.
5. **Policy layer**: `@PreAuthorize(...)` checks role permissions at controller methods.
6. **Error layer**: `RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler` (403).

If mentor asks "where security actually happens?"
- Authentication setup happens in `SecurityConfig`
- JWT parsing/checking happens in `JwtAuthenticationFilter` + `JwtService`
- Role checks happen in `@PreAuthorize`

---

## 3) End-to-end flow

### 3.1 Register flow (`POST /api/v1/auth/register`)
1. `AuthController.register(...)` receives validated DTO.
2. `AuthService.register(...)`:
   - checks email/phone uniqueness
   - loads default USER role
   - creates `User`
   - delegates to `UserServiceImpl.createUser(...)`
3. `UserServiceImpl.createUser(...)` hashes password using BCrypt before save.
4. User stored with role in DB.

### 3.2 Login flow (`POST /api/v1/auth/login`)
1. `AuthController.login(...)` receives email + password.
2. `AuthService.login(...)` calls `AuthenticationManager.authenticate(...)`.
3. Spring internally uses `CustomUserDetailsService.loadUserByUsername(...)` to fetch user + password hash + roles.
4. If credentials are valid, `JwtService.generateAccessToken(...)` and `JwtService.generateRefreshToken(...)` create JWTs.
5. Refresh token is stored in Redis with TTL using key format `refresh:<token>`.
6. `AuthResponseDto` returns access token, refresh token, and both expirations.

### 3.3 Refresh flow (`POST /api/v1/auth/refresh`)
1. Client sends refresh token in request body.
2. `AuthService.refresh(...)` parses username from refresh JWT.
3. Service validates JWT type/expiry using `JwtService.isRefreshTokenValid(...)`.
4. Service checks Redis key `refresh:<token>` exists and maps to same user email.
5. If valid, service issues a new short-lived access token.
6. Refresh token is not rotated in this implementation.

### 3.4 Protected API call flow (example: GET `/api/v1/users`)
1. Client sends `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` executes before username/password filter.
3. Filter extracts JWT and username.
4. Filter loads user details from DB.
5. Filter validates token and checks `userDetails.isEnabled()`.
6. On success, filter sets `SecurityContext` authentication.
7. Controller method has `@PreAuthorize(...)`:
   - if user has required authority -> request proceeds
   - else -> `RestAccessDeniedHandler` returns 403.
8. If no/invalid token -> `RestAuthenticationEntryPoint` returns 401.

---

## 4) Security folder file-by-file explanation

## 4.1 `src/main/java/com/example/Voucher/security/SecurityConfig.java`
### Responsibility
Central security configuration for Spring Security.

### Key methods
- `securityFilterChain(HttpSecurity http)`
  - `csrf().disable()` for stateless REST APIs.
  - `sessionCreationPolicy(STATELESS)` ensures no server-side session.
  - `httpBasic().disable()` and `formLogin().disable()` enforce JWT-only model.
  - Configures custom 401 and 403 handlers.
  - Permits `/api/v1/auth/**` and Swagger paths.
  - Requires authentication for all other paths.
  - Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

- `passwordEncoder()`
  - Returns `BCryptPasswordEncoder`.
  - Prevents storing plain passwords.

- `authenticationManager(...)`
  - Exposes Spring authentication manager bean used by `AuthService.login(...)`.

### Important keywords/annotations
- `@Configuration`: defines Spring config class.
- `@EnableWebSecurity`: enables web security support.
- `@EnableMethodSecurity`: enables `@PreAuthorize` checks.
- `@EnableConfigurationProperties(...)`: binds `JwtProperties`, `RoleProperties`.
- `SessionCreationPolicy.STATELESS`: each request must carry token.

### Mentor reasoning answer
"We keep security stateless and token-based by disabling session/basic/form auth and forcing JWT validation per request."

---

## 4.2 `src/main/java/com/example/Voucher/security/JwtAuthenticationFilter.java`
### Responsibility
Authenticates requests using JWT in `Authorization` header.

### Key methods
- `shouldNotFilter(HttpServletRequest request)`
  - Skips auth and docs endpoints (`/api/v1/auth/**`, swagger docs).

- `doFilterInternal(...)`
  - Reads header.
  - If missing/invalid format -> continue chain without authentication.
  - Extracts token and username via `JwtService`.
  - Loads user via `UserDetailsService`.
  - Validates token and checks user is enabled.
  - Creates `UsernamePasswordAuthenticationToken` with authorities.
  - Sets authentication in `SecurityContextHolder`.

### Important keywords
- `OncePerRequestFilter`: filter runs once per request.
- `SecurityContextHolder`: stores authenticated principal for current request thread.
- `UsernamePasswordAuthenticationToken`: authentication object used by Spring Security.

### Security reasoning
- No token => unauthenticated request (later becomes 401 on protected endpoint).
- Invalid token => authentication not set.
- Disabled user => authentication not set (important fix added).

---

## 4.3 `src/main/java/com/example/Voucher/security/JwtService.java`
### Responsibility
Generates and validates access and refresh JWT.

### Key methods
- `generateAccessToken(UserDetails userDetails)`
  - Creates signed token with subject (email), issuer, issue time, expiry, JWT ID.

- `generateRefreshToken(UserDetails userDetails)`
  - Creates signed refresh token with refresh-token expiry.

- `extractUsername(String token)`
  - Parses claims and returns JWT `sub`.

- `isAccessTokenValid(String token, UserDetails userDetails)`
  - Valid when username matches and token not expired.

- `isRefreshTokenValid(String token, UserDetails userDetails)`
  - Valid when token type is `refresh`, username matches, and token not expired.

- `normalizeSecret(String secret)`
  - Ensures signing key is valid for HS256.
  - If secret < 32 bytes, hashes to 256-bit key and logs warning.

### Important keywords
- `Jwts.builder()`: builds signed JWT.
- `subject`: principal identity (email here).
- `issuer`: who issued token.
- `expiration`: token expiry (replay window control).
- `HS256`: HMAC SHA-256 signature algorithm.

### Mentor reasoning answer
"Token trust is based on signature + expiry + subject match with loaded user details."

---

## 4.4 `src/main/java/com/example/Voucher/security/JwtProperties.java`
### Responsibility
Reads JWT config from `application.properties` (`security.jwt.*`).

### Behavior
- Provides `secret`, `issuer`, `accessExpirationSeconds`, `refreshExpirationSeconds`.
- Fallback logic: if `accessExpirationSeconds` missing/0, use `expirationSeconds`.

### Important keywords
- `@ConfigurationProperties(prefix = "security.jwt")`

---

## 4.5 `src/main/java/com/example/Voucher/security/CustomUserDetailsService.java`
### Responsibility
Loads user auth data for Spring Security.

### Key method
- `loadUserByUsername(String email)`
  - Fetches `User` from DB via `UserRepository.findByEmail(...)`.
  - Maps to Spring `UserDetails`:
    - username = email
    - password = password hash
    - disabled flag from `user.enabled`
    - authorities mapped from role names

### Important keywords
- `UserDetailsService`: core Spring auth interface.
- `SimpleGrantedAuthority`: authority item used in authorization checks.
- `@Transactional(readOnly = true)`: read optimization and consistency.

### Mentor reasoning answer
"This class is the bridge between DB user model and Spring Security’s authentication engine."

---

## 4.6 `src/main/java/com/example/Voucher/security/RestAuthenticationEntryPoint.java`
### Responsibility
Returns JSON 401 when request is unauthenticated.

### Key method
- `commence(...)`
  - Writes standardized `ApiError` response:
    - status 401
    - message "Authentication required"

### Why important
Without this, default Spring response may be HTML/basic; API clients need consistent JSON.

---

## 4.7 `src/main/java/com/example/Voucher/security/RestAccessDeniedHandler.java`
### Responsibility
Returns JSON 403 when user is authenticated but not authorized.

### Key method
- `handle(...)`
  - Writes standardized `ApiError` response:
    - status 403
    - message "Access denied"

### Why important
Clearly separates auth failure (401) from permission failure (403).

---

## 4.8 `src/main/java/com/example/Voucher/security/RoleProperties.java`
### Responsibility
Reads role names from config.

### Behavior
- Reads:
  - `security.roles.admin`
  - `security.roles.user`

### Why important
Role names are centralized and reusable in `@PreAuthorize` expressions.

---

## 4.9 `src/main/java/com/example/Voucher/security/RoleSeeder.java`
### Responsibility
Ensures core roles exist at app startup.

### Key methods
- `run(...)`
  - seeds admin + user role if missing.
- `seedRoleIfMissing(...)`
  - checks by name and inserts when absent.

### Why important
Prevents login/register failures due to missing role rows.

---

## 5) Related auth files outside `security/`

## 5.1 `src/main/java/com/example/Voucher/controller/AuthController.java`
### Responsibility
Public endpoints:
- `/register`
- `/login`
- `/refresh`
- `/logout`

### Methods
- `register(...)`: validates input, delegates to service, returns 201.
- `login(...)`: validates input, returns access + refresh tokens.
- `refresh(...)`: validates refresh token and issues new access token.
- `logout(...)`: deletes refresh token key from Redis.

---

## 5.2 `src/main/java/com/example/Voucher/service/AuthService.java`
### Responsibility
Core auth business logic.

### Methods
- `register(...)`
  - uniqueness checks
  - loads USER role
  - creates domain user
  - saves with password encoding through `UserService`

- `login(...)`
  - authenticates credentials via `AuthenticationManager`
  - loads `UserDetails`
  - creates JWT access and refresh tokens
  - stores refresh token in Redis with TTL (`refresh:<token>`)

- `refresh(...)`
  - validates refresh JWT and Redis presence
  - issues new access token
  - does not rotate refresh token

- `logout(...)`
  - deletes Redis key `refresh:<token>`

### Security reasoning
- No manual password comparison in service.
- Authentication delegated to Spring Security stack (safe and standard).

---

## 5.3 `src/main/java/com/example/Voucher/serviceImpl/UserServiceImpl.java`
### Security-relevant method
- `createUser(User user)`
  - hashes password via `PasswordEncoder` before DB save.

### Reasoning
Never store raw password. Hashing is done at user creation boundary.

---

## 5.4 `src/main/java/com/example/Voucher/repository/UserRepository.java`
### Security-relevant behavior
- `findByEmail(...)` uses `@EntityGraph(attributePaths = "roles")`.

### Why this matters
Ensures roles are loaded with user for auth checks, reducing lazy-loading issues during security operations.

---

## 5.5 `src/main/java/com/example/Voucher/repository/RoleRepository.java`
### Security-relevant behavior
- `findByName(...)` used in register + role seeding.

---

## 5.6 `src/main/java/com/example/Voucher/service/CurrentUserService.java`
### Responsibility
Current user identity and ownership checks.

### Key methods
- `getCurrentUser()` -> loads authenticated user object.
- `getCurrentUserId()` -> convenience wrapper.
- `isCurrentUserAdmin()` -> checks admin authority.
- `assertSelfOrAdmin(Long userId)` -> ownership guard for user-specific data.

### Reasoning
This class prevents horizontal privilege escalation (user A trying to access user B data).

---

## 6) Role-based access in controllers

### Example patterns used
- `@PreAuthorize("hasAuthority(@roleProperties.getAdmin())")`
- `@PreAuthorize("hasAuthority(@roleProperties.getUser())")`
- `@PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")`

### Where
- `UserController`: admin-only user listing.
- `AdminVoucherController`: admin-only template management.
- `UserVoucherController`: user voucher operations.
- `BillController` and `TransactionController`: mixed role checks + ownership checks via `CurrentUserService`.

### Reasoning
- Method-level authorization is explicit and easy to review.
- Business-level ownership rule is handled in service helper (`assertSelfOrAdmin`).

---

## 7) Important keywords/annotations quick sheet
- `@PreAuthorize`: method-level authorization expression.
- `hasAuthority(...)`: exact authority string match.
- `SecurityContextHolder`: current authenticated principal context.
- `AuthenticationManager`: verifies credentials.
- `UserDetailsService`: loads security user details.
- `PasswordEncoder` / `BCryptPasswordEncoder`: password hashing.
- `@EntityGraph`: eager fetch relationships for auth use-case.
- `STATELESS`: no HTTP session.
- `AuthenticationEntryPoint`: handles unauthenticated access (401).
- `AccessDeniedHandler`: handles unauthorized access (403).

---

## 8) 401 vs 403 (very common mentor question)
- **401 Unauthorized**: user is not authenticated (missing/invalid token).
  - handled by `RestAuthenticationEntryPoint`.
- **403 Forbidden**: user is authenticated but lacks permission.
  - handled by `RestAccessDeniedHandler`.

---

## 9) Why this implementation is beginner-friendly and safe
### Beginner-friendly
- Clear file separation by responsibility.
- Refresh token flow is kept simple (Redis lookup, no rotation).
- Standard Spring Security patterns.

### Safe enough for project/interview level
- BCrypt password hashing.
- Signed JWT with expiry.
- Refresh token storage in Redis with TTL.
- Stateless API security.
- Centralized role config and role seeding.
- Explicit method-level authorization.
- Ownership checks for user-specific resources.
- Disabled-user token block in JWT filter.

---

## 10) What to say when mentor asks "how refresh token works here?"
You can answer:
"We issue a short-lived access token and a long-lived refresh token at login. The refresh token is cached in Redis using key `refresh:<token>` with TTL equal to refresh expiration. During refresh, we validate the JWT itself and verify the Redis key exists for that user, then we issue a new access token. On logout, we delete the Redis key to invalidate refresh immediately. We intentionally do not rotate refresh tokens in this version to keep behavior simple."

---

## 11) Suggested interview-style explanation script (2 minutes)
"In this project, authentication starts in `AuthService.login`, where Spring `AuthenticationManager` validates email/password against `CustomUserDetailsService`. On success, `JwtService` issues both access and refresh JWTs. Refresh tokens are cached in Redis with key `refresh:<token>` and TTL. For every protected request, `JwtAuthenticationFilter` extracts Bearer access token, validates it, checks user enabled status, and sets authentication in `SecurityContext`. Authorization is enforced via `@PreAuthorize` using role values from `RoleProperties`. We return JSON-based 401/403 using custom handlers. Passwords are hashed with BCrypt, and ownership checks are enforced through `CurrentUserService.assertSelfOrAdmin`."

---

## 12) Final self-check before mentor demo
1. Can you explain each step from login request to token creation?
2. Can you explain each step from protected API call to role check?
3. Can you clearly differentiate 401 and 403 in your code?
4. Can you show where password hashing happens?
5. Can you show where disabled users are blocked?
6. Can you show where ownership checks happen?

If you can answer these 6 confidently, you are ready.
