CREATE TABLE IF NOT EXISTS tenant_onboarding_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_name VARCHAR(255) NOT NULL,
    tenant_code VARCHAR(100) NOT NULL,
    admin_first_name VARCHAR(255) NOT NULL,
    admin_last_name VARCHAR(255) NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    admin_phone_number VARCHAR(20) NOT NULL,
    admin_password_hash VARCHAR(255) NOT NULL,
    notes VARCHAR(2000) NULL,
    status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(2000) NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    approved_tenant_id BIGINT NULL,
    approved_tenant_admin_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_tenant_onboarding_requests_status_created_at
    ON tenant_onboarding_requests (status, created_at);
CREATE INDEX idx_tenant_onboarding_requests_tenant_code
    ON tenant_onboarding_requests (tenant_code);
CREATE INDEX idx_tenant_onboarding_requests_admin_email
    ON tenant_onboarding_requests (admin_email);
