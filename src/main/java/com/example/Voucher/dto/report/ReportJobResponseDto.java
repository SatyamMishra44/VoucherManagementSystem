package com.example.Voucher.dto.report;

import com.example.Voucher.report.ReportJob;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportJobResponseDto {

    private Long reportJobId;
    private String reportType;
    private String targetType;
    private Long targetId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String recipientEmail;
    private String status;
    private String outputFilePath;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static ReportJobResponseDto fromEntity(ReportJob job) {
        ReportJobResponseDto dto = new ReportJobResponseDto();
        dto.setReportJobId(job.getId());
        dto.setReportType(job.getReportType().name());
        dto.setTargetType(job.getTargetType().name());
        dto.setTargetId(job.getTargetId());
        dto.setFromDate(job.getFromDate());
        dto.setToDate(job.getToDate());
        dto.setRecipientEmail(job.getRecipientEmail());
        dto.setStatus(job.getStatus().name());
        dto.setOutputFilePath(job.getOutputFilePath());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setCompletedAt(job.getCompletedAt());
        return dto;
    }

    public Long getReportJobId() {
        return reportJobId;
    }

    public void setReportJobId(Long reportJobId) {
        this.reportJobId = reportJobId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

