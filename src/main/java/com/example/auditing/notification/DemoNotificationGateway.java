package com.example.auditing.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoNotificationGateway implements NotificationGateway {
    private final boolean simulateFailure;

    public DemoNotificationGateway(@Value("${demo.notification.fail:false}") boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public void send(Long enrollmentId) {
        if (simulateFailure) {
            throw new IllegalStateException("Simulated notification provider outage");
        }
        // Demo only: an actual implementation would call a provider here.
    }
}
