package com.example.auditing.enrollment;

import com.example.auditing.support.AuditedOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final EnrollmentRepository repository;

    public EnrollmentService(EnrollmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    @AuditedOperation(action = "ENROLL", targetType = "SECTION")
    public Enrollment enroll(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("sectionId must be positive");
        }
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        if (repository.findBySectionIdAndStudentUsername(sectionId, user).isPresent()) {
            throw new IllegalStateException("Already enrolled in section " + sectionId);
        }
        return repository.saveAndFlush(new Enrollment(sectionId, user));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('STUDENT', 'REGISTRAR')")
    @AuditedOperation(action = "DROP", targetType = "ENROLLMENT")
    public Enrollment drop(Long enrollmentId) {
        Enrollment enrollment = ownedEnrollment(enrollmentId);
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalStateException("Enrollment already dropped");
        }
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        return repository.saveAndFlush(enrollment);
    }

    @Transactional
    @PreAuthorize("hasRole('REGISTRAR')")
    @AuditedOperation(action = "CHANGE_STATUS", targetType = "ENROLLMENT")
    public Enrollment changeStatus(Long enrollmentId, EnrollmentStatus newStatus) {
        Enrollment enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment " + enrollmentId));
        if (newStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        enrollment.setStatus(newStatus);
        return repository.saveAndFlush(enrollment);
    }

    private Enrollment ownedEnrollment(Long id) {
        Enrollment enrollment = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment " + id));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean registrar = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_REGISTRAR"));
        if (!registrar && !enrollment.getStudentUsername().equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your enrollment");
        }
        return enrollment;
    }
}
