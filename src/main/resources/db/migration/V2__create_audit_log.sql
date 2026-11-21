CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    principal VARCHAR(120) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(120) NULL,
    occurred_at DATETIME(6) NOT NULL,
    outcome VARCHAR(10) NOT NULL,
    failure_reason VARCHAR(1000) NULL,
    INDEX ix_audit_log_principal_occurred (principal, occurred_at),
    INDEX ix_audit_log_target (target_type, target_id)
);
