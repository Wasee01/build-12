package com.example.auditing.enrollment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findBySectionIdAndStudentUsername(Long sectionId, String studentUsername);
}
