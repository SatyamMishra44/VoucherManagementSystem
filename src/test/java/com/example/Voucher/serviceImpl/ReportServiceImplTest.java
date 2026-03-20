package com.example.Voucher.serviceImpl;

import com.example.Voucher.dto.report.ReportGenerationRequestDto;
import com.example.Voucher.dto.report.SelfReportEmailRequestDto;
import com.example.Voucher.entity.User;
import com.example.Voucher.report.ReportJob;
import com.example.Voucher.report.ReportJobProcessor;
import com.example.Voucher.report.ReportTargetType;
import com.example.Voucher.report.ReportType;
import com.example.Voucher.repository.ReportJobRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportJobRepository reportJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RoleProperties roleProperties;

    @Mock
    private ReportJobProcessor reportJobProcessor;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                reportJobRepository,
                userRepository,
                tenantRepository,
                roleProperties,
                reportJobProcessor
        );
    }

    @Test
    void requestReportGeneration_fromAfterTo_throws() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.USER, 1L);
        request.setFromDate(LocalDate.now());
        request.setToDate(LocalDate.now().minusDays(1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestReportGeneration(request, 99L)
        );
        assertTrue(ex.getMessage().contains("fromDate cannot be after toDate"));
        verifyNoInteractions(reportJobRepository);
    }

    @Test
    void requestReportGeneration_rangeTooLarge_throws() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.USER, 1L);
        request.setFromDate(LocalDate.now().minusDays(200));
        request.setToDate(LocalDate.now());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestReportGeneration(request, 99L)
        );
        assertTrue(ex.getMessage().contains("Date range cannot exceed 180 days"));
    }

    @Test
    void requestReportGeneration_targetUserNotFound_throws() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.USER, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestReportGeneration(request, 99L)
        );
        assertTrue(ex.getMessage().contains("Target user not found"));
    }

    @Test
    void requestReportGeneration_targetTenantNotFound_throws() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.TENANT, 5L);
        when(tenantRepository.findById(5L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestReportGeneration(request, 99L)
        );
        assertTrue(ex.getMessage().contains("Target tenant not found"));
    }

    @Test
    void requestReportGeneration_noRecipients_throws() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.TENANT, 5L);
        request.setRecipientEmail(null);
        request.setIncludeTenantAdmins(false);
        Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestReportGeneration(request, 99L)
        );
        assertTrue(ex.getMessage().contains("No recipient email resolved for report delivery"));
    }

    @Test
    void requestReportGeneration_userTarget_success() {
        ReportGenerationRequestDto request = buildReportRequest(ReportTargetType.USER, 1L);
        request.setRecipientEmail("extra@example.com");
        User user = buildUser(1L, "user@example.com", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(inv -> {
            ReportJob job = inv.getArgument(0);
            setReportJobId(job, 42L);
            return job;
        });

        ReportJob job = service.requestReportGeneration(request, 99L);

        assertNotNull(job);
        assertEquals(ReportType.USER_DETAILED, job.getReportType());
        assertEquals(ReportTargetType.USER, job.getTargetType());
        assertEquals(1L, job.getTargetId());
        assertEquals(99L, job.getRequestedByUserId());
        assertEquals("extra@example.com,user@example.com", job.getRecipientEmail());
        verify(reportJobProcessor).processReportJobAsync(42L);
    }

    @Test
    void requestMyUserReport_invalidTenantType_throws() {
        SelfReportEmailRequestDto request = buildSelfReportRequest();
        User user = buildUser(1L, "user@example.com", 11L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Tenant tenant = mockTenant(TenantType.ORGANIZATION);
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestMyUserReport(request, 1L)
        );
        assertTrue(ex.getMessage().contains("Organization users cannot request own reports directly"));
    }

    @Test
    void requestMyUserReport_success() {
        SelfReportEmailRequestDto request = buildSelfReportRequest();
        request.setRecipientEmail("extra@example.com");
        User user = buildUser(1L, "user@example.com", 11L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Tenant tenant = mockTenant(TenantType.SYSTEM_INDIVIDUAL);
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenant));
        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(inv -> {
            ReportJob job = inv.getArgument(0);
            setReportJobId(job, 100L);
            return job;
        });

        ReportJob job = service.requestMyUserReport(request, 1L);

        assertEquals(ReportType.USER_DETAILED, job.getReportType());
        assertEquals(ReportTargetType.USER, job.getTargetType());
        assertEquals(1L, job.getTargetId());
        assertEquals("user@example.com,extra@example.com", job.getRecipientEmail());
        verify(reportJobProcessor).processReportJobAsync(100L);
    }

    @Test
    void requestMyTenantReport_userNotFound_throws() {
        SelfReportEmailRequestDto request = buildSelfReportRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestMyTenantReport(request, 1L)
        );
        assertTrue(ex.getMessage().contains("Requesting user not found"));
    }

    @Test
    void requestMyTenantReport_success() {
        SelfReportEmailRequestDto request = buildSelfReportRequest();
        request.setRecipientEmail("extra@example.com");
        User user = buildUser(1L, "user@example.com", 11L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(inv -> {
            ReportJob job = inv.getArgument(0);
            setReportJobId(job, 200L);
            return job;
        });

        ReportJob job = service.requestMyTenantReport(request, 1L);

        assertEquals(ReportType.TENANT_DETAILED, job.getReportType());
        assertEquals(ReportTargetType.TENANT, job.getTargetType());
        assertEquals(11L, job.getTargetId());
        assertEquals("user@example.com,extra@example.com", job.getRecipientEmail());
        verify(reportJobProcessor).processReportJobAsync(200L);
    }

    @Test
    void getReportJob_notFound_throws() {
        when(reportJobRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getReportJob(1L)
        );
        assertTrue(ex.getMessage().contains("Report job not found"));
    }

    @Test
    void getReportJob_found_returns() {
        ReportJob job = new ReportJob(
                ReportType.USER_DETAILED,
                ReportTargetType.USER,
                1L,
                99L,
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "user@example.com"
        );
        when(reportJobRepository.findById(1L)).thenReturn(Optional.of(job));

        ReportJob result = service.getReportJob(1L);

        assertSame(job, result);
    }

    private ReportGenerationRequestDto buildReportRequest(ReportTargetType targetType, Long targetId) {
        ReportGenerationRequestDto request = new ReportGenerationRequestDto();
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        request.setFromDate(LocalDate.now().minusDays(7));
        request.setToDate(LocalDate.now());
        request.setRecipientEmail(null);
        request.setIncludeTenantAdmins(true);
        return request;
    }

    private SelfReportEmailRequestDto buildSelfReportRequest() {
        SelfReportEmailRequestDto request = new SelfReportEmailRequestDto();
        request.setFromDate(LocalDate.now().minusDays(7));
        request.setToDate(LocalDate.now());
        request.setRecipientEmail(null);
        return request;
    }

    private User buildUser(Long id, String email, Long tenantId) {
        User user = new User(
                "Test",
                "User",
                "hash",
                "1234567890",
                email,
                LocalDateTime.now()
        );
        user.setTenantId(tenantId);
        setUserId(user, id);
        return user;
    }

    private Tenant mockTenant(TenantType tenantType) {
        Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
        when(tenant.getTenantType()).thenReturn(tenantType);
        return tenant;
    }

    private void setReportJobId(ReportJob job, Long id) {
        try {
            Field field = ReportJob.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(job, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setUserId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
