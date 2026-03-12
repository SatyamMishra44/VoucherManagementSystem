CREATE TABLE IF NOT EXISTS tenant_voucher_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    voucher_template_id BIGINT NOT NULL,
    quantity_purchased_total INT NOT NULL,
    quantity_available INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_voucher_inventory_tenant_template UNIQUE (tenant_id, voucher_template_id),
    CONSTRAINT fk_tenant_voucher_inventory_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_voucher_inventory_template FOREIGN KEY (voucher_template_id) REFERENCES voucher_templates(id)
);

CREATE INDEX idx_tenant_voucher_inventory_tenant_id ON tenant_voucher_inventory (tenant_id);
CREATE INDEX idx_tenant_voucher_inventory_template_id ON tenant_voucher_inventory (voucher_template_id);

CREATE TABLE IF NOT EXISTS tenant_voucher_distributions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    tenant_voucher_inventory_id BIGINT NOT NULL,
    distributed_to_user_id BIGINT NOT NULL,
    distributed_by_user_id BIGINT NOT NULL,
    quantity_distributed INT NOT NULL,
    total_distributed_amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tenant_voucher_distributions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_voucher_distributions_inventory FOREIGN KEY (tenant_voucher_inventory_id) REFERENCES tenant_voucher_inventory(id),
    CONSTRAINT fk_tenant_voucher_distributions_to_user FOREIGN KEY (distributed_to_user_id) REFERENCES users(id),
    CONSTRAINT fk_tenant_voucher_distributions_by_user FOREIGN KEY (distributed_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_tenant_voucher_distributions_tenant_id ON tenant_voucher_distributions (tenant_id);
CREATE INDEX idx_tenant_voucher_distributions_created_at ON tenant_voucher_distributions (created_at);
