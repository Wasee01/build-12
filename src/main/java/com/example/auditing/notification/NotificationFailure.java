package com.example.auditing.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_failure")
public class NotificationFailure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;
    @Column(name = "failure_reason", nullable = false, length = 1000)
    private String failureReason;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected NotificationFailure() { }
    public NotificationFailure(Long enrollmentId, String failureReason) {
        this.enrollmentId = enrollmentId;
        this.failureReason = failureReason;
        this.recordedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public String getFailureReason() { return failureReason; }
    public Instant getRecordedAt() { return recordedAt; }
}
