package com.example.auditing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auditing.enrollment.Enrollment;
import com.example.auditing.enrollment.EnrollmentRepository;
import com.example.auditing.enrollment.EnrollmentService;
import com.example.auditing.enrollment.EnrollmentStatus;
import com.example.auditing.notification.NotificationFailureRepository;
import com.example.auditing.notification.NotificationGateway;
import com.example.auditing.notification.NotificationService;
import com.example.auditing.support.AuditLog;
import com.example.auditing.support.AuditLogRepository;
import com.example.auditing.support.AuditOutcome;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Real MySQL test: Flyway, proxy ordering, REQUIRES_NEW, and Spring Retry. */
@SpringBootTest
class EnrollmentAuditIntegrationTest {
    private Long testId;
    @Autowired EnrollmentService enrollments;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired AuditLogRepository audits;
    @Autowired NotificationService notifications;
    @Autowired NotificationFailureRepository notificationFailures;
    @Autowired TestGateway gateway;
    @Autowired JdbcTemplate jdbc;

    @TestConfiguration
    static class GatewayTestConfig {
        @Bean
        @Primary
        TestGateway testGateway() { return new TestGateway(); }
    }

    static class TestGateway implements NotificationGateway {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean fail;
        @Override
        public void send(Long enrollmentId) {
            calls.incrementAndGet();
            if (fail) throw new IllegalStateException("remote unavailable");
        }
    }

    @BeforeEach
    void asAlice() {
        testId = ThreadLocalRandom.current().nextLong(1L << 50, Long.MAX_VALUE);
        gateway.calls.set(0);
        gateway.fail = false;
        authenticate("alice", "ROLE_STUDENT");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        jdbc.update("""
                DELETE FROM audit_log WHERE target_type = 'ENROLLMENT' AND target_id IN (
                    SELECT CAST(id AS CHAR) FROM enrollment
                    WHERE section_id = ? AND student_username = 'alice'
                )
                """, testId);
        jdbc.update("DELETE FROM audit_log WHERE target_type = 'SECTION' AND target_id IN (?, ?)",
                testId.toString(), Long.toString(-testId));
        jdbc.update("DELETE FROM enrollment WHERE section_id = ? AND student_username = 'alice'", testId);
        jdbc.update("DELETE FROM notification_failure WHERE enrollment_id = ?", testId);
    }

    private static void authenticate(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(name, "n/a",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void successfulEnrollmentHasOneAuditWithAliceFromContext() {
        Long section = testId;
        Enrollment enrollment = enrollments.enroll(section);

        assertThat(enrollment.getId()).isNotNull();
        var matchingAudits = audits.findAll().stream()
                .filter(a -> "SECTION".equals(a.getTargetType()))
                .filter(a -> section.toString().equals(a.getTargetId()))
                .toList();
        assertThat(matchingAudits).hasSize(1);
        AuditLog audit = matchingAudits.getFirst();
        assertThat(audit.getPrincipal()).isEqualTo("alice");
        assertThat(audit.getAction()).isEqualTo("ENROLL");
        assertThat(audit.getTargetType()).isEqualTo("SECTION");
        assertThat(audit.getTargetId()).isEqualTo(section.toString());
        assertThat(audit.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    }

    @Test
    void failedTransactionPersistsFailureAuditButNoEnrollment() {
        Long invalidSection = -testId;
        assertThatThrownBy(() -> enrollments.enroll(invalidSection))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(enrollmentRepository.findBySectionIdAndStudentUsername(invalidSection, "alice")).isEmpty();
        var matchingAudits = audits.findAll().stream()
                .filter(a -> "SECTION".equals(a.getTargetType()))
                .filter(a -> invalidSection.toString().equals(a.getTargetId()))
                .toList();
        assertThat(matchingAudits).hasSize(1);
        AuditLog audit = matchingAudits.getFirst();
        assertThat(audit.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(audit.getFailureReason()).contains("sectionId must be positive");
    }

    @Test
    void dropAndStatusChangeAreAlsoAudited() {
        Enrollment enrollment = enrollments.enroll(testId);
        enrollments.drop(enrollment.getId());
        authenticate("registrar", "ROLE_REGISTRAR");
        enrollments.changeStatus(enrollment.getId(), EnrollmentStatus.WAITLISTED);
        assertThat(audits.findAll().stream()
                .filter(a -> "ENROLLMENT".equals(a.getTargetType()))
                .filter(a -> enrollment.getId().toString().equals(a.getTargetId()))
                .map(AuditLog::getAction))
                .containsExactlyInAnyOrder("DROP", "CHANGE_STATUS");
    }

    @Test
    void retryThreeTimesThenRecoverWithoutPropagating() {
        gateway.fail = true;
        Long notificationId = testId;
        notifications.sendEnrollmentNotice(notificationId);
        assertThat(gateway.calls.get()).isEqualTo(3);
        assertThat(notificationFailures.findAll().stream()
                .filter(f -> notificationId.equals(f.getEnrollmentId()))
                .toList()).singleElement().satisfies(failure ->
                        assertThat(failure.getFailureReason()).contains("remote unavailable"));
    }
}
