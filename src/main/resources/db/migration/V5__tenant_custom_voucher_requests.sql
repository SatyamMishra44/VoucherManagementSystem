CREATE TABLE IF NOT EXISTS tenant_voucher_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    requested_voucher_code VARCHAR(255) NOT NULL,
    requested_unit_value DECIMAL(19,2) NOT NULL,
    requested_start_date DATE NOT NULL,
    requested_expiry_date DATE NOT NULL,
    notes VARCHAR(2000) NULL,
    status VARCHAR(32) NOT NULL,
    platform_comment VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tenant_voucher_requests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_voucher_requests_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_tenant_voucher_requests_tenant_id ON tenant_voucher_requests (tenant_id);
CREATE INDEX idx_tenant_voucher_requests_status ON tenant_voucher_requests (status);
CREATE INDEX idx_tenant_voucher_requests_created_at ON tenant_voucher_requests (created_at);
