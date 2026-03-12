# Report Generation + Email Delivery (Beginner Implementation Guide)

This guide explains exactly how to implement report generation with PDF attachment + email delivery in a Spring Boot project.

It is written so you can build the feature from zero without extra help.

---

## 1. What You Are Building

You are implementing a background job flow:

1. User calls API to request report.
2. System stores a `report_jobs` row as `QUEUED`.
3. Async worker picks job and marks `IN_PROGRESS`.
4. Worker loads report data from DB.
5. Worker generates PDF file.
6. Worker sends email with PDF attachment.
7. Worker marks job `COMPLETED` or `FAILED`.

---

## 2. Final API Surface

1. `POST /api/v1/reports/me/email` for normal user (`USER`)
2. `POST /api/v1/reports/tenant/email` for tenant admin (`TENANT_ADMIN`)
3. `GET /api/v1/reports/{reportJobId}` to check job status

Required headers:

- `Authorization: Bearer <token>`
- `X-Tenant-Code: <tenant-code>` (for multi-tenant context)

---

## 3. Dependencies

Add these to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
</dependency>
```

---

## 4. Database Migration

Create Flyway file:
`src/main/resources/db/migration/V6__report_jobs.sql`

```sql
CREATE TABLE IF NOT EXISTS report_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    report_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    output_file_path VARCHAR(1000) NULL,
    error_message VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_jobs_requested_by_user
      FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_report_jobs_status_created_at ON report_jobs (status, created_at);
CREATE INDEX idx_report_jobs_target ON report_jobs (target_type, target_id);
CREATE INDEX idx_report_jobs_requested_by ON report_jobs (requested_by_user_id, created_at);
```

Run migration and verify table exists.

---

## 5. Domain Model

Create these enums/classes in `src/main/java/.../report`:

1. `ReportType` (`USER_DETAILED`, `TENANT_DETAILED`)
2. `ReportTargetType` (`USER`, `TENANT`)
3. `ReportJobStatus` (`QUEUED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`)
4. `ReportJob` entity (maps to `report_jobs`)
5. `ReportRenderData` (DTO used by PDF generator)

`ReportJob` should expose state methods:

1. `markInProgress()`
2. `markCompleted(outputFilePath)`
3. `markFailed(errorMessage)`

---

## 6. Repositories (Critical for Avoiding Lazy Errors)

Create:

- `ReportJobRepository extends JpaRepository<ReportJob, Long>`

Extend existing repos with date-range queries for report data.

Important: use eager loading for relations used in PDF generation.

In `TransactionRepository`, on report methods add:

```java
@EntityGraph(attributePaths = {"user", "bill"})
```

In `UserVoucherRepository`, on report methods add:

```java
@EntityGraph(attributePaths = {"voucherTemplate", "user"})
```

Without this, PDF generation can fail with:
`LazyInitializationException: ... no session`

---

## 7. Config

Add to `application.properties`:

```properties
report.storage.path=${REPORT_STORAGE_PATH:./generated-reports}
report.email.from=${REPORT_EMAIL_FROM:no-reply@voucher.local}

spring.mail.properties.mail.smtp.connectiontimeout=${MAIL_SMTP_CONNECTION_TIMEOUT_MS:5000}
spring.mail.properties.mail.smtp.timeout=${MAIL_SMTP_TIMEOUT_MS:5000}
spring.mail.properties.mail.smtp.writetimeout=${MAIL_SMTP_WRITE_TIMEOUT_MS:5000}

report.job.pdf-timeout-ms=${REPORT_PDF_TIMEOUT_MS:30000}
report.job.email-timeout-ms=${REPORT_EMAIL_TIMEOUT_MS:30000}
```

Add secret SMTP config in `config/application-secrets.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

For Gmail, use App Password (not normal account password).

---

## 8. Enable Async

Create:
`src/main/java/.../config/AsyncConfig.java`

```java
@Configuration
@EnableAsync
public class AsyncConfig {}
```

---

## 9. Email Service

Create `ReportEmailService`:

1. Inject `JavaMailSender` (or `ObjectProvider<JavaMailSender>`).
2. Build `MimeMessage`.
3. Set `from`, `to`, `subject`, `text`.
4. Attach PDF file.
5. Send.
6. Wrap and rethrow meaningful error.

---

## 10. PDF Generator

Create `ReportPdfGenerator`:

1. Ensure output directory exists (`Files.createDirectories`).
2. Build HTML content from `ReportRenderData`.
3. Render HTML to PDF using `PdfRendererBuilder`.
4. Save file as `report-<jobId>.pdf`.
5. Return `Path`.

---

## 11. Service Layer

Create interface `ReportService` and implementation `ReportServiceImpl`.

Responsibilities:

1. Validate input:
   - `fromDate <= toDate`
   - max range (example: 180 days)
2. Resolve scope by caller:
   - `/me/email` -> current user
   - `/tenant/email` -> current tenant
3. Resolve recipients:
   - always include requester email
   - include optional `recipientEmail`
4. Insert `ReportJob` with status `QUEUED`
5. Dispatch async processing **after transaction commit**

Use `TransactionSynchronizationManager.registerSynchronization(... afterCommit ...)` so worker does not run before row is committed.

---

## 12. Async Processor

Create `ReportJobProcessor` with `@Async`.

Execution steps:

1. Load job by id.
2. Mark `IN_PROGRESS`.
3. Load scoped transactions + vouchers.
4. Build summary metrics.
5. Generate PDF with timeout.
6. Send email with timeout.
7. Mark `COMPLETED` with file path.
8. If any exception, mark `FAILED`.

Use resilient failure saving:

1. Build bounded error message.
2. Save failed state.
3. If failed-state save also fails, do fallback failed save with shorter message.

This prevents jobs getting stuck forever in `IN_PROGRESS`.

---

## 13. Controller

Create `ReportController`:

1. `POST /api/v1/reports/me/email` with `@PreAuthorize("hasAuthority(@roleProperties.getUser())")`
2. `POST /api/v1/reports/tenant/email` with `TENANT_ADMIN`
3. `GET /api/v1/reports/{id}` for status

Return `202 Accepted` for create endpoints with `ReportJobResponseDto`.

---

## 14. DTOs

Create:

1. `SelfReportEmailRequestDto`
   - `fromDate` (`@NotNull`)
   - `toDate` (`@NotNull`)
   - `recipientEmail` (`@Email`, optional)
2. `ReportJobResponseDto` mapped from `ReportJob`

---

## 15. End-to-End Test Flow

1. Start app.
2. Login and get access token.
3. Call:

```bash
curl -X POST http://localhost:8080/api/v1/reports/me/email \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Tenant-Code: SYSTEM_INDIVIDUAL" \
  -H "Content-Type: application/json" \
  -d '{
    "fromDate":"2026-03-01",
    "toDate":"2026-03-09",
    "recipientEmail":"myarchive@example.com"
  }'
```

4. Poll status:

```bash
curl -X GET http://localhost:8080/api/v1/reports/<JOB_ID> \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Tenant-Code: SYSTEM_INDIVIDUAL"
```

5. Verify:
   - status becomes `COMPLETED`
   - file exists under `generated-reports`
   - email received with attachment

---

## 16. Troubleshooting (Most Common)

### A) Job stuck in `IN_PROGRESS`

Cause:
failure occurred and failed-state persistence also failed.

Fix:
implement resilient failed-status persistence in processor.

### B) `LazyInitializationException ... no session`

Cause:
PDF code accessed lazy relation after session closed.

Fix:
add `@EntityGraph` for report queries to fetch needed relations eagerly.

### C) SMTP auth failures

Cause:
wrong mail credentials or using normal Gmail password.

Fix:
use app password + correct host/port/auth/starttls.

### D) Timeout failures

Cause:
SMTP/PDF too slow.

Fix:
increase timeout properties and check network.

---

## 17. Production Hardening

1. Move PDFs to object storage (S3/GCS).
2. Add retry strategy for transient mail errors.
3. Add dead-letter mechanism for permanent failures.
4. Add scheduler to recover stale `QUEUED`/`IN_PROGRESS` jobs on restart.
5. Add metrics and alerts on failure rate.
6. Add cleanup job for old generated files.

---

## 18. Implementation Order (Do This Exactly)

1. Add dependencies.
2. Add migration and run it.
3. Create enums + `ReportJob` entity.
4. Create repository interfaces.
5. Add `@EntityGraph` to report queries.
6. Add config properties + secrets.
7. Add async config.
8. Build PDF generator.
9. Build email service.
10. Build report service (`create QUEUED` + afterCommit dispatch).
11. Build async processor (`IN_PROGRESS` -> `COMPLETED/FAILED`).
12. Build controller + DTOs.
13. Run end-to-end curl flow.
14. Validate DB state and mailbox delivery.

If you complete these 14 steps in order, the feature works reliably.
