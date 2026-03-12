CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(100) NOT NULL,
    tenant_name VARCHAR(255) NOT NULL,
    tenant_type VARCHAR(32) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_tenants_tenant_code UNIQUE (tenant_code)
);

INSERT INTO tenants (tenant_code, tenant_name, tenant_type, active, created_at)
VALUES ('SYSTEM_INDIVIDUAL', 'System Individual Customers', 'SYSTEM_INDIVIDUAL', b'1', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE tenant_code = VALUES(tenant_code);

SET @db_name = DATABASE();
SET @system_tenant_id = (SELECT id FROM tenants WHERE tenant_code = 'SYSTEM_INDIVIDUAL' LIMIT 1);

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE users ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'users' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE voucher_templates ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'voucher_templates' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE user_vouchers ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_vouchers' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE bills ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'bills' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE transactions ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE redemption_history ADD COLUMN tenant_id BIGINT NULL',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'redemption_history' AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE users SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;
UPDATE voucher_templates SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;
UPDATE user_vouchers uv
JOIN users u ON u.id = uv.user_id
SET uv.tenant_id = u.tenant_id
WHERE uv.tenant_id IS NULL;
UPDATE user_vouchers SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;
UPDATE bills b
JOIN users u ON u.id = b.user_id
SET b.tenant_id = u.tenant_id
WHERE b.tenant_id IS NULL;
UPDATE bills SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;
UPDATE transactions t
JOIN users u ON u.id = t.user_id
SET t.tenant_id = u.tenant_id
WHERE t.tenant_id IS NULL;
UPDATE transactions SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;
UPDATE redemption_history rh
JOIN user_vouchers uv ON uv.id = rh.user_voucher_id
SET rh.tenant_id = uv.tenant_id
WHERE rh.tenant_id IS NULL;
UPDATE redemption_history SET tenant_id = @system_tenant_id WHERE tenant_id IS NULL;

ALTER TABLE users MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE voucher_templates MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE user_vouchers MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE bills MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE transactions MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE redemption_history MODIFY COLUMN tenant_id BIGINT NOT NULL;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_users_tenant_id ON users (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_voucher_templates_tenant_id ON voucher_templates (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'voucher_templates' AND INDEX_NAME = 'idx_voucher_templates_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_user_vouchers_tenant_id ON user_vouchers (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_vouchers' AND INDEX_NAME = 'idx_user_vouchers_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_bills_tenant_id ON bills (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'bills' AND INDEX_NAME = 'idx_bills_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_transactions_tenant_id ON transactions (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_transactions_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_redemption_history_tenant_id ON redemption_history (tenant_id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'redemption_history' AND INDEX_NAME = 'idx_redemption_history_tenant_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'users' AND CONSTRAINT_NAME = 'fk_users_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE voucher_templates ADD CONSTRAINT fk_voucher_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'voucher_templates' AND CONSTRAINT_NAME = 'fk_voucher_templates_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE user_vouchers ADD CONSTRAINT fk_user_vouchers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'user_vouchers' AND CONSTRAINT_NAME = 'fk_user_vouchers_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE bills ADD CONSTRAINT fk_bills_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'bills' AND CONSTRAINT_NAME = 'fk_bills_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE transactions ADD CONSTRAINT fk_transactions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND CONSTRAINT_NAME = 'fk_transactions_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE redemption_history ADD CONSTRAINT fk_redemption_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name AND TABLE_NAME = 'redemption_history' AND CONSTRAINT_NAME = 'fk_redemption_history_tenant'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE roles SET name = 'PLATFORM_ADMIN', description = 'Platform administrator' WHERE name = 'ADMIN';
INSERT IGNORE INTO roles (name, description) VALUES ('PLATFORM_ADMIN', 'Platform administrator');
INSERT IGNORE INTO roles (name, description) VALUES ('TENANT_ADMIN', 'Tenant administrator');
INSERT IGNORE INTO roles (name, description) VALUES ('USER', 'Standard user');
