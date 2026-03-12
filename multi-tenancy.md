# Multi-Tenancy Guide (Current Implementation)

This document describes how multi-tenancy is currently implemented in this project.
It is based on the running code and migrations in this repository.

Scope date: March 6, 2026.

## 1. Tenancy Model
- Model: shared database + shared schema using `tenant_id`
- Tenant types:
- `SYSTEM_INDIVIDUAL`
- `ORGANIZATION`

`SYSTEM_INDIVIDUAL` is the default/fallback tenant used for legacy or non-tenant-specific flows.

## 2. Tenant Resolution Rules
Tenant context is resolved per request by `TenantContextFilter`.

Resolution logic:
1. If authenticated user exists, use tenant from principal (`TenantAwareUserDetails.tenantId`).
2. If header `X-Tenant-Code` is also provided and does not match authenticated tenant, return `403 Tenant mismatch`.
3. If unauthenticated and header exists, resolve using header tenant code.
4. If unauthenticated and header is missing, fallback to `SYSTEM_INDIVIDUAL`.
5. Tenant must be active.

Important:
- For authenticated APIs, JWT tenant is source of truth.
- `X-Tenant-Code` is mainly needed for unauthenticated auth routing (`register/login/refresh`) to non-default tenant.

## 3. Security and Tenant Binding
### 3.1 JWT
Access token includes `tenant_id` claim.

### 3.2 Authentication
- `JwtAuthenticationFilter` extracts `sub` and `tenant_id`.
- User is loaded by `(email, tenantId)`.
- Token is valid only if tenant matches loaded user tenant.

### 3.3 Refresh Token
Redis key format:
- `refresh:{tenantId}:{token}`

Redis value format:
- `{tenantId}:{email}`

Refresh flow enforces tenant match and Redis subject match, blocking cross-tenant reuse.

## 4. Data Isolation Strategy
Isolation exists at multiple layers:
- Request layer: tenant context filter
- Security layer: tenant claim in JWT
- Service layer: `TenantContext.requireTenantId()` usage
- Repository layer: tenant-scoped methods (`...AndTenantId`)
- Entity layer: cross-tenant defensive constructor checks
- DB layer: `tenant_id` non-null + foreign keys + indexes

## 5. Tenant-Owned Tables
Tenant-owned business data includes:
- `users`
- `voucher_templates`
- `user_vouchers`
- `bills`
- `transactions`
- `redemption_history`
- `tenant_voucher_inventory`
- `tenant_voucher_distributions`
- `tenant_voucher_requests`

Platform governance tables include:
- `tenants`
- `tenant_audit_logs`

## 6. Role Model in Multi-Tenant Context
- `PLATFORM_ADMIN`: platform governance, tenant lifecycle, template lifecycle
- `TENANT_ADMIN`: organization inventory purchase/distribution and custom request creation
- `USER`: user-level voucher redemption and personal history

## 7. Endpoint Behavior by Tenant Context
### 7.1 Platform-only
- `/api/v1/platform/tenants/**`
- `/api/v1/admin/vouchers` create/status

### 7.2 Tenant admin organization operations
- `POST /api/v1/tenant/vouchers/purchase`
- `GET /api/v1/tenant/vouchers/inventory`
- `POST /api/v1/tenant/vouchers/distribute`
- `POST /api/v1/tenant/vouchers/requests`

### 7.3 User/tenant catalog and redemption
- `GET /api/v1/vouchers` accessible to `USER` and `TENANT_ADMIN`

Catalog behavior:
- If tenant is `ORGANIZATION`, eligible templates are sourced from platform tenant (`SYSTEM_INDIVIDUAL`) so org can browse platform catalog.
- If tenant is `SYSTEM_INDIVIDUAL`, templates are sourced from same tenant.

## 8. Organization Flow
1. Platform admin onboards organization tenant and first tenant admin.
2. Tenant admin logs in under org tenant.
3. Tenant admin views existing vouchers (`GET /api/v1/vouchers`).
4. Tenant admin buys stock (`POST /api/v1/tenant/vouchers/purchase`).
5. Tenant admin distributes stock to org users (`POST /api/v1/tenant/vouchers/distribute`).
6. Users redeem assigned vouchers (`POST /api/v1/vouchers/redeem`).
7. If no suitable voucher exists, tenant admin creates custom request (`POST /api/v1/tenant/vouchers/requests`).

## 9. Migration Timeline
- `V1__baseline_schema.sql`: baseline domain schema + legacy roles
- `V2__multitenancy_bootstrap.sql`: tenants table, tenant backfill, tenant FKs/indexes, role transition
- `V3__tenant_audit_logs.sql`: tenant audit trail table
- `V4__tenant_voucher_inventory.sql`: tenant voucher inventory + distribution tables
- `V5__tenant_custom_voucher_requests.sql`: custom voucher request table

## 10. Known Gaps
1. Platform-side APIs for reviewing/approving/rejecting custom voucher requests are not yet implemented.
2. Some uniqueness constraints are globally unique (`email`, `phone`, voucher `code`), which can conflict with tenant-local expectations.
3. Legacy direct user purchase endpoint still exists and may need product decision relative to strict tenant-admin procurement model.

## 11. Non-Negotiable Rules
- Never trust header-only tenant identity for authenticated users.
- Always include tenant filters in tenant-owned queries.
- Always set tenant id on tenant-owned writes.
- Reject tenant mismatch early.
- Keep `SYSTEM_INDIVIDUAL` deactivation blocked.
