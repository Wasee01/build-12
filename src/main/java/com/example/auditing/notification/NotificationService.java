package com.example.auditing.notification;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationGateway gateway;
    private final NotificationFailureRepository failures;

    public NotificationService(NotificationGateway gateway, NotificationFailureRepository failures) {
        this.gateway = gateway;
        this.failures = failures;
    }

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 1.0))
    public void sendEnrollmentNotice(Long enrollmentId) {
        gateway.send(enrollmentId);
    }

    @Recover
    public void recover(RuntimeException error, Long enrollmentId) {
        String reason = error.getClass().getSimpleName() + ": " + error.getMessage();
        failures.save(new NotificationFailure(enrollmentId,
                reason.length() > 1000 ? reason.substring(0, 1000) : reason));
    }
}
