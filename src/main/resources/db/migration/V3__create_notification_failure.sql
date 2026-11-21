CREATE TABLE notification_failure (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    failure_reason VARCHAR(1000) NOT NULL,
    recorded_at DATETIME(6) NOT NULL
);
