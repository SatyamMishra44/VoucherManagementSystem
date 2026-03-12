# Voucher Management System - Backend Technical Documentation

## 1. Purpose
This document explains the backend as implemented in this repository as of March 6, 2026.
It covers architecture, security, tenancy model, API surface, organization workflow, data model, and operational gaps.

## 2. System Summary
Spring Boot backend for voucher operations with shared-schema multi-tenancy.

Primary capabilities:
- Auth (`register`, `login`, `refresh`, `logout`) using JWT + Redis-backed refresh validation
- Platform-managed voucher templates
- Organization tenant onboarding and lifecycle management
- Tenant-admin voucher stock purchase and distribution to users
- User voucher redemption, billing, transactions, and redemption history

## 3. Tech Stack
- Java 21, Spring Boot 3.4.2
- Spring Web, Spring Security, Spring Data JPA
- MySQL + Flyway
- Redis (`StringRedisTemplate`)
- JJWT
- springdoc OpenAPI

## 4. Architecture
Layers:
- `controller`: REST endpoints + authz annotations
- `service/serviceImpl`: business logic
- `repository`: tenant-scoped DB access
- `entity`: JPA domain model + invariants
- `security`: JWT filters/services, RBAC config
- `tenant`: tenant resolution/context
- `platform`: platform governance

### 4.1 Request Pipeline
1. `JwtAuthenticationFilter` authenticates token and loads principal (`TenantAwareUserDetails`).
2. `TenantContextFilter` resolves tenant from authenticated principal, then optional header fallback.
3. Controller authorization (`@PreAuthorize`).
4. Service + repository enforce business + tenant boundaries.

### 4.2 Architecture Diagram Placeholder

`[Architecture Diagram - High Level Components]`





`[Architecture Diagram - Request Flow (JWT -> TenantContext -> Controller -> Service -> Repository)]`





## 5. Multi-Tenancy Model
- Model: shared DB + shared schema, `tenant_id` on business tables
- Tenant types:
  - `SYSTEM_INDIVIDUAL` (platform/default)
  - `ORGANIZATION`

Tenant resolution:
- For authenticated requests: token principal tenant is source of truth
- `X-Tenant-Code` mismatch with authenticated principal => `403 Tenant mismatch`
- For unauthenticated auth flows, header can route to non-default tenant

## 6. Security and Token Model
### 6.1 Roles
- `PLATFORM_ADMIN`
- `TENANT_ADMIN`
- `USER`

### 6.2 Access Token Claims
Access token includes:
- `jti`
- `iss`
- `sub` (email)
- `token_type=access`
- `iat`
- `exp`
- `tenant_id`

### 6.3 Refresh Token Validation
Redis key/value model:
- key: `refresh:{tenantId}:{token}`
- value: `{tenantId}:{email}`

Refresh validation checks token parse/type/expiry, tenant match, user enabled, and Redis subject match.

## 7. API Surface (Current)
### 7.1 Auth (Public)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### 7.2 Platform Governance
- `GET /api/v1/users` (`PLATFORM_ADMIN`, `TENANT_ADMIN`, tenant-scoped)
- `POST /api/v1/platform/tenants/organizations` (`PLATFORM_ADMIN`)
- `PATCH /api/v1/platform/tenants/{tenantId}/status` (`PLATFORM_ADMIN`)
- `POST /api/v1/platform/tenants/{tenantId}/tenant-admins` (`PLATFORM_ADMIN`)
- `GET /api/v1/platform/tenants` (`PLATFORM_ADMIN`)
- `GET /api/v1/platform/tenants/{tenantId}/audit-logs` (`PLATFORM_ADMIN`)

### 7.3 Voucher Template Lifecycle (Platform-only)
- `POST /api/v1/admin/vouchers` (`PLATFORM_ADMIN`)
- `PATCH /api/v1/admin/vouchers/{templateId}/status` (`PLATFORM_ADMIN`)

### 7.4 Voucher Catalog + User Operations
- `GET /api/v1/vouchers` (`USER`, `TENANT_ADMIN`)
  - For `ORGANIZATION` tenant: returns eligible templates from platform catalog (`SYSTEM_INDIVIDUAL` tenant templates)
- `POST /api/v1/vouchers/purchase` (`USER`)
- `POST /api/v1/vouchers/redeem` (`USER`)
- `GET /api/v1/vouchers/mine` (`USER`)
- `GET /api/v1/vouchers/redemptions` (`USER`)

### 7.5 Tenant Admin Voucher Stock APIs
- `POST /api/v1/tenant/vouchers/purchase` (`TENANT_ADMIN`)
  - Buy stock from platform catalog into org inventory
- `GET /api/v1/tenant/vouchers/inventory` (`TENANT_ADMIN`)
  - View org stock
- `POST /api/v1/tenant/vouchers/distribute` (`TENANT_ADMIN`)
  - Allocate stock to org user (creates user voucher)
- `POST /api/v1/tenant/vouchers/requests` (`TENANT_ADMIN`)
  - Raise custom voucher request for platform review

### 7.6 Bills/Transactions
- `POST /api/v1/bills` (`PLATFORM_ADMIN`, `TENANT_ADMIN`)
- `GET /api/v1/bills/{billId}` (admin or owner user)
- `GET /api/v1/bills/user/{userId}` (admin or same user)
- `GET /api/v1/transactions/user/{userId}` (admin or same user)

## 8. Organization Flow (Implemented)
1. Platform creates organization tenant + tenant admin.
2. Tenant admin logs in under org tenant.
3. Tenant admin views existing vouchers via `GET /api/v1/vouchers`.
4. If suitable template exists:
- buy stock: `POST /api/v1/tenant/vouchers/purchase`
- view stock: `GET /api/v1/tenant/vouchers/inventory`
- distribute: `POST /api/v1/tenant/vouchers/distribute`
5. Organization users redeem assigned vouchers via `POST /api/v1/vouchers/redeem`.
6. If suitable template does not exist:
- tenant admin submits custom request: `POST /api/v1/tenant/vouchers/requests`
- request persists as `PENDING` for platform action.

## 9. Data Model and Migrations
Key entities:
- Core: `Tenant`, `User`, `Role`, `VoucherTemplate`, `UserVoucher`, `Bill`, `Transaction`, `RedemptionHistory`, `TenantAuditLog`
- New org flow: `TenantVoucherInventory`, `TenantVoucherDistribution`, `TenantVoucherRequest`

Flyway:
- `V1__baseline_schema.sql`
- `V2__multitenancy_bootstrap.sql`
- `V3__tenant_audit_logs.sql`
- `V4__tenant_voucher_inventory.sql`
- `V5__tenant_custom_voucher_requests.sql`

## 10. Business Rules
- Voucher template must be enabled and within valid date range.
- Redemption deducts `min(billAmount, remainingBalance)`.
- Voucher row lock (`PESSIMISTIC_WRITE`) protects against concurrent over-redemption.
- Cross-tenant assignment/transaction/history combinations are blocked by constructor invariants and tenant-scoped queries.
- Tenant stock distribution consumes inventory then creates user voucher allocation.

## 11. Error Contract
Standard error response:
- `status`, `error`, `message`, `timestamp`

Common statuses:
- `400` validation/business errors
- `401` authentication/invalid refresh
- `403` forbidden/tenant mismatch
- `503` DB/Redis unavailable
- `500` fallback

## 12. Testing Status
Current test suite (unit + mvc slices) passes with latest changes:
- `./mvnw -q test -DskipITs`

Coverage exists for core auth/service error paths and voucher redemption behavior.
Integration coverage for full platform custom-request approval workflow is still pending.

## 13. Open Gaps / Next Steps
1. Platform-side custom request review endpoints are not implemented yet (approve/reject/create-template workflow).
2. Global uniqueness constraints (`users.email`, `users.phone_number`, `voucher_templates.code`) may conflict with tenant-local business assumptions.
3. Clarify final product direction for legacy `USER` direct purchase endpoint vs strict tenant-admin procurement model.
4. Add explicit reporting endpoints for tenant distribution history.
