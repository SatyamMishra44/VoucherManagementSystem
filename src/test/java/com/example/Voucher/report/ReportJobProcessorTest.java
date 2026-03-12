package com.example.Voucher.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Voucher.repository.ReportJobRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportJobProcessorTest {

    private static final int EXPECTED_MAX_ERROR_LENGTH = 1800;

    private final ReportJobRepository reportJobRepository = Mockito.mock(ReportJobRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final TenantRepository tenantRepository = Mockito.mock(TenantRepository.class);
    private final TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
    private final UserVoucherRepository userVoucherRepository = Mockito.mock(UserVoucherRepository.class);
    private final ReportPdfGenerator reportPdfGenerator = Mockito.mock(ReportPdfGenerator.class);
    private final ReportEmailService reportEmailService = Mockito.mock(ReportEmailService.class);

    @Test
    void processReportJobAsync_whenEmailFails_marksJobFailedWithTruncatedMessage() throws Exception {
        ReportJob job = buildTenantJob(1L, 10L);
        Tenant tenant = new Tenant("tenant-a", "Tenant A", TenantType.ORGANIZATION);
        setField(tenant, "id", 10L);

        when(reportJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(transactionRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(userVoucherRepository.findByTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());
        Path pdf = Files.createTempFile("report-job-", ".pdf");
        when(reportPdfGenerator.generatePdf(anyLong(), any())).thenReturn(pdf);
        when(reportJobRepository.saveAndFlush(any(ReportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("x".repeat(5000)))
                .when(reportEmailService).sendReportEmail(any(), any(), any(), any());

        ReportJobProcessor processor = new ReportJobProcessor(
                reportJobRepository,
                userRepository,
                tenantRepository,
                transactionRepository,
                userVoucherRepository,
                reportPdfGenerator,
                reportEmailService,
                1000L,
                1000L
        );

        processor.processReportJobAsync(1L);

        assertEquals(ReportJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertTrue(job.getErrorMessage().length() <= EXPECTED_MAX_ERROR_LENGTH);
        verify(reportJobRepository, times(2)).saveAndFlush(any(ReportJob.class));
    }

    @Test
    void processReportJobAsync_whenFailureSaveThrows_retriesFailedStatusPersistence() throws Exception {
        ReportJob job = buildTenantJob(2L, 20L);
        Tenant tenant = new Tenant("tenant-b", "Tenant B", TenantType.ORGANIZATION);
        setField(tenant, "id", 20L);

        when(reportJobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(tenantRepository.findById(20L)).thenReturn(Optional.of(tenant));
        when(transactionRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(userVoucherRepository.findByTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());
        Path pdf = Files.createTempFile("report-job-", ".pdf");
        when(reportPdfGenerator.generatePdf(anyLong(), any())).thenReturn(pdf);
        doThrow(new RuntimeException("smtp timeout"))
                .when(reportEmailService).sendReportEmail(any(), any(), any(), any());

        AtomicInteger saveCount = new AtomicInteger();
        when(reportJobRepository.saveAndFlush(any(ReportJob.class))).thenAnswer(invocation -> {
            int attempt = saveCount.incrementAndGet();
            if (attempt == 2) {
                throw new RuntimeException("Data too long for column 'error_message'");
            }
            return invocation.getArgument(0);
        });

        ReportJobProcessor processor = new ReportJobProcessor(
                reportJobRepository,
                userRepository,
                tenantRepository,
                transactionRepository,
                userVoucherRepository,
                reportPdfGenerator,
                reportEmailService,
                1000L,
                1000L
        );

        processor.processReportJobAsync(2L);

        assertEquals(ReportJobStatus.FAILED, job.getStatus());
        assertTrue(job.getErrorMessage().contains("could not be stored"));
        verify(reportJobRepository, times(3)).saveAndFlush(any(ReportJob.class));
    }

    private ReportJob buildTenantJob(Long jobId, Long tenantId) {
        ReportJob job = new ReportJob(
                ReportType.TENANT_DETAILED,
                ReportTargetType.TENANT,
                tenantId,
                101L,
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "report@example.com"
        );
        setField(job, "id", jobId);
        return job;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
