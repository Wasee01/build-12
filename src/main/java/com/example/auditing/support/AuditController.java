package com.example.auditing.support;

import com.example.auditing.notification.NotificationFailure;
import com.example.auditing.notification.NotificationFailureRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('REGISTRAR')")
public class AuditController {
    private final AuditLogRepository audit;
    private final NotificationFailureRepository failures;

    public AuditController(AuditLogRepository audit, NotificationFailureRepository failures) {
        this.audit = audit;
        this.failures = failures;
    }

    @GetMapping("/audits")
    public List<AuditLog> audits() { return audit.findAll(); }

    @GetMapping("/notification-failures")
    public List<NotificationFailure> notificationFailures() { return failures.findAll(); }
}
