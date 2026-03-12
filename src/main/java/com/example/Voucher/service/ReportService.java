package com.example.Voucher.service;

import com.example.Voucher.dto.report.ReportGenerationRequestDto;
import com.example.Voucher.dto.report.SelfReportEmailRequestDto;
import com.example.Voucher.report.ReportJob;

public interface ReportService {

    ReportJob requestReportGeneration(ReportGenerationRequestDto request, Long requestedByUserId);

    ReportJob requestMyUserReport(SelfReportEmailRequestDto request, Long requestedByUserId);

    ReportJob requestMyTenantReport(SelfReportEmailRequestDto request, Long requestedByUserId);

    ReportJob getReportJob(Long reportJobId);
}
