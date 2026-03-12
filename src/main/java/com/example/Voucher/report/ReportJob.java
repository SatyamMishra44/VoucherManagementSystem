package com.example.Voucher.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_jobs")
public class ReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 32)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReportJobStatus status;

    @Column(name = "output_file_path", length = 1000)
    private String outputFilePath;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected ReportJob() {
    }

    public ReportJob(
            ReportType reportType,
            ReportTargetType targetType,
            Long targetId,
            Long requestedByUserId,
            LocalDate fromDate,
            LocalDate toDate,
            String recipientEmail
    ) {
        this.reportType = reportType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.requestedByUserId = requestedByUserId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.recipientEmail = recipientEmail;
        this.status = ReportJobStatus.QUEUED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public ReportJobStatus getStatus() {
        return status;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void markInProgress() {
        this.status = ReportJobStatus.IN_PROGRESS;
        this.errorMessage = null;
    }

    public void markCompleted(String outputFilePath) {
        this.status = ReportJobStatus.COMPLETED;
        this.outputFilePath = outputFilePath;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ReportJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}

