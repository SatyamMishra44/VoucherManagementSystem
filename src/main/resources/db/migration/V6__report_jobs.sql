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
    CONSTRAINT fk_report_jobs_requested_by_user FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_report_jobs_status_created_at ON report_jobs (status, created_at);
CREATE INDEX idx_report_jobs_target ON report_jobs (target_type, target_id);
CREATE INDEX idx_report_jobs_requested_by ON report_jobs (requested_by_user_id, created_at);
