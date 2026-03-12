package com.example.Voucher.serviceImpl;

import com.example.Voucher.dto.report.ReportGenerationRequestDto;
import com.example.Voucher.dto.report.SelfReportEmailRequestDto;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.ReportJobRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.report.ReportJob;
import com.example.Voucher.report.ReportJobProcessor;
import com.example.Voucher.report.ReportTargetType;
import com.example.Voucher.report.ReportType;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.service.ReportService;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportJobRepository reportJobRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleProperties roleProperties;
    private final ReportJobProcessor reportJobProcessor;

    public ReportServiceImpl(
            ReportJobRepository reportJobRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RoleProperties roleProperties,
            ReportJobProcessor reportJobProcessor
    ) {
        this.reportJobRepository = reportJobRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleProperties = roleProperties;
        this.reportJobProcessor = reportJobProcessor;
    }

    @Override
    @Transactional
    public ReportJob requestReportGeneration(ReportGenerationRequestDto request, Long requestedByUserId) {
        validateRequest(request);

        String recipients = resolveRecipients(
                request.getTargetType(),
                request.getTargetId(),
                request.getRecipientEmail(),
                Boolean.TRUE.equals(request.getIncludeTenantAdmins())
        );

        ReportJob reportJob = new ReportJob(
                resolveReportType(request.getTargetType()),
                request.getTargetType(),
                request.getTargetId(),
                requestedByUserId,
                request.getFromDate(),
                request.getToDate(),
                recipients
        );
        reportJob = reportJobRepository.save(reportJob);
        dispatchAfterCommit(reportJob.getId());
        return reportJob;
    }

    @Override
    @Transactional
    public ReportJob requestMyUserReport(SelfReportEmailRequestDto request, Long requestedByUserId) {
        validateDateRange(request.getFromDate(), request.getToDate());
        User requester = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Requesting user not found"));
        Tenant tenant = tenantRepository.findById(requester.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Requesting user tenant not found"));
        if (!TenantType.SYSTEM_INDIVIDUAL.equals(tenant.getTenantType())) {
            throw new IllegalArgumentException(
                    "Organization users cannot request own reports directly. Contact your tenant admin."
            );
        }
        return createAndQueueSelfReport(
                ReportType.USER_DETAILED,
                ReportTargetType.USER,
                requester.getId(),
                requester.getEmail(),
                request,
                requestedByUserId
        );
    }

    @Override
    @Transactional
    public ReportJob requestMyTenantReport(SelfReportEmailRequestDto request, Long requestedByUserId) {
        validateDateRange(request.getFromDate(), request.getToDate());
        User requester = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Requesting user not found"));
        return createAndQueueSelfReport(
                ReportType.TENANT_DETAILED,
                ReportTargetType.TENANT,
                requester.getTenantId(),
                requester.getEmail(),
                request,
                requestedByUserId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReportJob getReportJob(Long reportJobId) {
        return reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Report job not found"));
    }

    private void validateRequest(ReportGenerationRequestDto request) {
        validateDateRange(request.getFromDate(), request.getToDate());
    }

    private ReportType resolveReportType(ReportTargetType targetType) {
        return targetType == ReportTargetType.USER
                ? ReportType.USER_DETAILED
                : ReportType.TENANT_DETAILED;
    }

    private String resolveRecipients(
            ReportTargetType targetType,
            Long targetId,
            String recipientEmail,
            boolean includeTenantAdmins
    ) {
        Set<String> recipients = new LinkedHashSet<>();
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            recipients.add(recipientEmail.trim());
        }

        if (targetType == ReportTargetType.USER) {
            User user = userRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
            recipients.add(user.getEmail());
        } else {
            Tenant tenant = tenantRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Target tenant not found"));
            if (includeTenantAdmins) {
                List<User> tenantAdmins = userRepository.findAllByTenantIdAndRoles_Name(
                        tenant.getId(),
                        roleProperties.getTenantAdmin()
                );
                for (User admin : tenantAdmins) {
                    recipients.add(admin.getEmail());
                }
            }
        }

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("No recipient email resolved for report delivery");
        }

        return String.join(",", new ArrayList<>(recipients));
    }

    private void validateDateRange(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > 180) {
            throw new IllegalArgumentException("Date range cannot exceed 180 days");
        }
    }

    private ReportJob createAndQueueSelfReport(
            ReportType reportType,
            ReportTargetType targetType,
            Long targetId,
            String requesterEmail,
            SelfReportEmailRequestDto request,
            Long requestedByUserId
    ) {
        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(requesterEmail);
        if (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank()) {
            recipients.add(request.getRecipientEmail().trim());
        }

        ReportJob reportJob = new ReportJob(
                reportType,
                targetType,
                targetId,
                requestedByUserId,
                request.getFromDate(),
                request.getToDate(),
                String.join(",", recipients)
        );
        reportJob = reportJobRepository.save(reportJob);
        dispatchAfterCommit(reportJob.getId());
        return reportJob;
    }

    private void dispatchAfterCommit(Long reportJobId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reportJobProcessor.processReportJobAsync(reportJobId);
                }
            });
            return;
        }
        reportJobProcessor.processReportJobAsync(reportJobId);
    }

}
