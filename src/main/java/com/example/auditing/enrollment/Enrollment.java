package com.example.auditing.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "enrollment")
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "student_username", nullable = false, length = 120)
    private String studentUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    protected Enrollment() { }

    public Enrollment(Long sectionId, String studentUsername) {
        this.sectionId = sectionId;
        this.studentUsername = studentUsername;
        this.status = EnrollmentStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public Long getSectionId() { return sectionId; }
    public String getStudentUsername() { return studentUsername; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
}
