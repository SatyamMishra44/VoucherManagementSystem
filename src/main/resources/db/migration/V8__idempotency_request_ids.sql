-- Add request_id column to bills, user_vouchers, transactions, and redemption_history
ALTER TABLE bills ADD COLUMN request_id VARCHAR(255) NULL;
ALTER TABLE user_vouchers ADD COLUMN request_id VARCHAR(255) NULL;
ALTER TABLE transactions ADD COLUMN request_id VARCHAR(255) NULL;
ALTER TABLE redemption_history ADD COLUMN request_id VARCHAR(255) NULL;

-- Add unique constraints on (tenant_id, request_id)
ALTER TABLE bills ADD CONSTRAINT uk_bills_tenant_request UNIQUE (tenant_id, request_id);
ALTER TABLE user_vouchers ADD CONSTRAINT uk_user_vouchers_tenant_request UNIQUE (tenant_id, request_id);
ALTER TABLE transactions ADD CONSTRAINT uk_transactions_tenant_request UNIQUE (tenant_id, request_id);
ALTER TABLE redemption_history ADD CONSTRAINT uk_redemption_history_tenant_request UNIQUE (tenant_id, request_id);
