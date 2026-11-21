package com.example.auditing.notification;

/** Boundary for a real SMS/email provider. Replace this demo with an HTTP adapter. */
public interface NotificationGateway {
    void send(Long enrollmentId);
}
