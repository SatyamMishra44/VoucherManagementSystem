CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS voucher_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    unit_value DECIMAL(19,2) NOT NULL,
    start_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_voucher_templates_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS user_vouchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    voucher_template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    quantity_purchased INT NOT NULL,
    total_purchased_amount DECIMAL(19,2) NOT NULL,
    remaining_balance DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    purchased_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_vouchers_template FOREIGN KEY (voucher_template_id) REFERENCES voucher_templates(id),
    CONSTRAINT fk_user_vouchers_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bills_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bill_id BIGINT NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    final_amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_transactions_bill FOREIGN KEY (bill_id) REFERENCES bills(id)
);

CREATE TABLE IF NOT EXISTS redemption_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_voucher_id BIGINT NOT NULL,
    bill_id BIGINT NULL,
    redeemed_amount DECIMAL(19,2) NOT NULL,
    remaining_balance_after DECIMAL(19,2) NOT NULL,
    redeemed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_redemption_history_user_voucher FOREIGN KEY (user_voucher_id) REFERENCES user_vouchers(id),
    CONSTRAINT fk_redemption_history_bill FOREIGN KEY (bill_id) REFERENCES bills(id)
);

INSERT IGNORE INTO roles (name, description) VALUES ('ADMIN', 'System administrator');
INSERT IGNORE INTO roles (name, description) VALUES ('USER', 'Standard user');
