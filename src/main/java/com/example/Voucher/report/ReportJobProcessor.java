package com.example.Voucher.report;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.UserVoucherStatus;
import com.example.Voucher.repository.ReportJobRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReportJobProcessor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1800;
    private static final Logger log = LoggerFactory.getLogger(ReportJobProcessor.class);

    private final ReportJobRepository reportJobRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TransactionRepository transactionRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final ReportPdfGenerator reportPdfGenerator;
    private final ReportEmailService reportEmailService;
    private final long pdfTimeoutMs;
    private final long emailTimeoutMs;

    public ReportJobProcessor(
            ReportJobRepository reportJobRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TransactionRepository transactionRepository,
            UserVoucherRepository userVoucherRepository,
            ReportPdfGenerator reportPdfGenerator,
            ReportEmailService reportEmailService,
            @Value("${report.job.pdf-timeout-ms:30000}") long pdfTimeoutMs,
            @Value("${report.job.email-timeout-ms:30000}") long emailTimeoutMs
    ) {
        this.reportJobRepository = reportJobRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.transactionRepository = transactionRepository;
        this.userVoucherRepository = userVoucherRepository;
        this.reportPdfGenerator = reportPdfGenerator;
        this.reportEmailService = reportEmailService;
        this.pdfTimeoutMs = pdfTimeoutMs;
        this.emailTimeoutMs = emailTimeoutMs;
    }

    @Async
    public void processReportJobAsync(Long reportJobId) {
        long startedAt = System.currentTimeMillis();
        ReportJob job = reportJobRepository.findById(reportJobId).orElse(null);
        if (job == null) {
            log.warn("Report job {} not found for async processing", reportJobId);
            return;
        }

        try {
            log.info("Report job {} processing started", reportJobId);
            job.markInProgress();
            reportJobRepository.saveAndFlush(job);

            LocalDateTime fromDateTime = job.getFromDate().atStartOfDay();
            LocalDateTime toDateTime = job.getToDate().atTime(23, 59, 59);

            ScopeData scopeData = loadScopeData(job, fromDateTime, toDateTime);
            ReportRenderData reportData = buildRenderData(job, scopeData);
            Path outputPath = CompletableFuture.supplyAsync(() -> reportPdfGenerator.generatePdf(job.getId(), reportData))
                    .get(pdfTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("Report job {} PDF generated at {}", reportJobId, outputPath);

            List<String> recipients = Arrays.stream(job.getRecipientEmail().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            CompletableFuture.runAsync(() -> reportEmailService.sendReportEmail(
                            recipients,
                            outputPath.toFile(),
                            "Voucher Report " + job.getId() + " (" + job.getReportType().name() + ")",
                            "Please find attached report " + job.getId()
                                    + " for date range " + job.getFromDate() + " to " + job.getToDate() + "."
                    ))
                    .get(emailTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("Report job {} email sent to {}", reportJobId, recipients);

            job.markCompleted(outputPath.toString());
            reportJobRepository.saveAndFlush(job);
            log.info("Report job {} completed in {} ms", reportJobId, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            markFailedSafely(job, ex);
            log.error("Report job {} failed after {} ms: {}", reportJobId, System.currentTimeMillis() - startedAt, ex.getMessage(), ex);
        }
    }

    private void markFailedSafely(ReportJob job, Exception ex) {
        String failureMessage = buildFailureMessage(ex);
        try {
            job.markFailed(failureMessage);
            reportJobRepository.saveAndFlush(job);
        } catch (Exception persistenceEx) {
            log.error("Failed to persist report job {} failure details: {}", job.getId(), persistenceEx.getMessage(), persistenceEx);
            try {
                String fallbackMessage = truncate("Job failed, and detailed error could not be stored: "
                        + buildFailureMessage(persistenceEx));
                job.markFailed(fallbackMessage);
                reportJobRepository.saveAndFlush(job);
            } catch (Exception fallbackPersistenceEx) {
                log.error("Unable to persist FAILED status for report job {}. Job may remain IN_PROGRESS.",
                        job.getId(), fallbackPersistenceEx);
            }
        }
    }

    private String buildFailureMessage(Throwable throwable) {
        StringBuilder message = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 6) {
            if (!message.isEmpty()) {
                message.append(" | caused by: ");
            }
            message.append(current.getClass().getSimpleName());
            String currentMessage = current.getMessage();
            if (currentMessage != null && !currentMessage.isBlank()) {
                message.append(": ").append(currentMessage.trim());
            }
            current = current.getCause();
            depth++;
        }
        if (message.isEmpty()) {
            return "Unknown processing error";
        }
        return truncate(message.toString());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private ScopeData loadScopeData(ReportJob job, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (job.getTargetType() == ReportTargetType.USER) {
            User user = userRepository.findById(job.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Target tenant not found"));

            List<Transaction> transactions = transactionRepository.findByUserIdAndTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                    user.getId(),
                    tenant.getId(),
                    fromDateTime,
                    toDateTime
            );
            List<UserVoucher> vouchers = userVoucherRepository.findByUserIdAndTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(
                    user.getId(),
                    tenant.getId(),
                    fromDateTime,
                    toDateTime
            );
            String label = "User ID: " + user.getId() + ", Email: " + user.getEmail()
                    + ", Tenant: " + tenant.getTenantCode();
            return new ScopeData(label, transactions, vouchers);
        }

        Tenant tenant = tenantRepository.findById(job.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Target tenant not found"));

        List<Transaction> transactions = transactionRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                tenant.getId(),
                fromDateTime,
                toDateTime
        );
        List<UserVoucher> vouchers = userVoucherRepository.findByTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(
                tenant.getId(),
                fromDateTime,
                toDateTime
        );
        String label = "Tenant ID: " + tenant.getId() + ", Code: " + tenant.getTenantCode()
                + ", Name: " + tenant.getTenantName();
        return new ScopeData(label, transactions, vouchers);
    }

    private ReportRenderData buildRenderData(ReportJob job, ScopeData scopeData) {
        List<Transaction> transactions = scopeData.transactions();
        List<UserVoucher> vouchers = scopeData.vouchers();

        BigDecimal grossAmount = transactions.stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalAmount = transactions.stream()
                .map(Transaction::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal redeemedAmount = grossAmount.subtract(finalAmount);

        long fullyRedeemedCount = vouchers.stream()
                .filter(v -> v.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0)
                .count();
        long partiallyRedeemedCount = vouchers.stream()
                .filter(v -> v.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0
                        && v.getRemainingBalance().compareTo(v.getTotalPurchasedAmount()) < 0)
                .count();
        long notRedeemedCount = vouchers.stream()
                .filter(v -> v.getRemainingBalance().compareTo(v.getTotalPurchasedAmount()) == 0)
                .count();
        long activeCount = vouchers.stream()
                .filter(v -> v.getStatus() == UserVoucherStatus.ACTIVE)
                .count();
        long inactiveCount = vouchers.stream()
                .filter(v -> v.getStatus() == UserVoucherStatus.INACTIVE)
                .count();

        BigDecimal totalPurchasedAmount = vouchers.stream()
                .map(UserVoucher::getTotalPurchasedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemainingAmount = vouchers.stream()
                .map(UserVoucher::getRemainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String title = job.getTargetType() == ReportTargetType.USER
                ? "Individual User Voucher Statement"
                : "Tenant Voucher Utilization Report";

        return new ReportRenderData(
                title,
                scopeData.label(),
                job.getFromDate(),
                job.getToDate(),
                LocalDateTime.now().format(DATE_TIME_FORMATTER),
                transactions.size(),
                grossAmount,
                finalAmount,
                redeemedAmount,
                vouchers.size(),
                fullyRedeemedCount,
                partiallyRedeemedCount,
                notRedeemedCount,
                activeCount,
                inactiveCount,
                totalPurchasedAmount,
                totalRemainingAmount,
                transactions,
                vouchers
        );
    }

    private record ScopeData(String label, List<Transaction> transactions, List<UserVoucher> vouchers) {
    }
}
