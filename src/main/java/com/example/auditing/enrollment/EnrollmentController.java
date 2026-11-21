package com.example.auditing.enrollment;

import com.example.auditing.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService service;
    private final EnrollmentRepository repository;
    private final NotificationService notifications;

    public EnrollmentController(EnrollmentService service, EnrollmentRepository repository,
                                NotificationService notifications) {
        this.service = service;
        this.repository = repository;
        this.notifications = notifications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Enrollment enroll(@RequestBody EnrollRequest request) {
        Enrollment result = service.enroll(request.sectionId());
        // Notification failure is recovered, so this never changes committed enrollment.
        notifications.sendEnrollmentNotice(result.getId());
        return result;
    }

    @PostMapping("/{id}/drop")
    public Enrollment drop(@PathVariable Long id) {
        return service.drop(id);
    }

    @PutMapping("/{id}/status")
    public Enrollment updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'REGISTRAR')")
    public Enrollment get(@PathVariable Long id) {
        Enrollment result = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment " + id));
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_REGISTRAR"))
                && !result.getStudentUsername().equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your enrollment");
        }
        return result;
    }

    public record EnrollRequest(Long sectionId) { }
    public record StatusRequest(EnrollmentStatus status) { }
}
