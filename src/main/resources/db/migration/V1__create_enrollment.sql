CREATE TABLE enrollment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    student_username VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_enrollment_student_section UNIQUE (student_username, section_id)
);
