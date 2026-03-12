package com.example.Voucher.controller;

import com.example.Voucher.dto.report.ReportJobResponseDto;
import com.example.Voucher.dto.report.SelfReportEmailRequestDto;
import com.example.Voucher.report.ReportJob;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. Reports", description = "Platform report generation and email delivery APIs")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    public ReportController(
            ReportService reportService,
            CurrentUserService currentUserService
    ) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/me/email")
    @PreAuthorize("hasAuthority(@roleProperties.getUser())")
    @Operation(
            summary = "User: Generate Own Report and Email",
            description = "Generates a USER report for the authenticated user (system individual users only)."
    )
    public ResponseEntity<ReportJobResponseDto> generateMyUserReport(
            @Valid @RequestBody SelfReportEmailRequestDto request
    ) {
        ReportJob job = reportService.requestMyUserReport(request, currentUserService.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReportJobResponseDto.fromEntity(job));
    }

    @PostMapping("/tenant/email")
    @PreAuthorize("hasAuthority(@roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Tenant Admin: Generate Organization Report and Email",
            description = "Generates a TENANT report for the authenticated admin's organization."
    )
    public ResponseEntity<ReportJobResponseDto> generateMyTenantReport(
            @Valid @RequestBody SelfReportEmailRequestDto request
    ) {
        ReportJob job = reportService.requestMyTenantReport(request, currentUserService.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReportJobResponseDto.fromEntity(job));
    }

    @GetMapping("/{reportJobId}")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: Report Job Status",
            description = "Returns status and metadata for a report generation job."
    )
    public ResponseEntity<ReportJobResponseDto> getReportJobStatus(@PathVariable Long reportJobId) {
        ReportJob job = reportService.getReportJob(reportJobId);
        return ResponseEntity.ok(ReportJobResponseDto.fromEntity(job));
    }
}
