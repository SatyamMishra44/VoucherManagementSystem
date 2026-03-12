CREATE TABLE IF NOT EXISTS tenant_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    details VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tenant_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX idx_tenant_audit_logs_tenant_id ON tenant_audit_logs (tenant_id);
CREATE INDEX idx_tenant_audit_logs_created_at ON tenant_audit_logs (created_at);
