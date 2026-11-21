package com.example.auditing.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principal", nullable = false, length = 120)
    private String principal;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id", length = 120)
    private String targetId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 10)
    private AuditOutcome outcome;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    protected AuditLog() { }

    public AuditLog(String principal, String action, String targetType, String targetId,
                    Instant occurredAt, AuditOutcome outcome, String failureReason) {
        this.principal = principal;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.occurredAt = occurredAt;
        this.outcome = outcome;
        this.failureReason = failureReason;
    }

    public Long getId() { return id; }
    public String getPrincipal() { return principal; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public Instant getOccurredAt() { return occurredAt; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getFailureReason() { return failureReason; }
}
