package com.example.auditing.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditAspectTest {
    private AuditWriter writer;
    private AuditAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private AuditedOperation annotation;

    @BeforeEach
    void setUp() {
        writer = mock(AuditWriter.class);
        aspect = new AuditAspect(writer);
        joinPoint = mock(ProceedingJoinPoint.class);
        annotation = mock(AuditedOperation.class);
        when(annotation.action()).thenReturn("ENROLL");
        when(annotation.targetType()).thenReturn("SECTION");
        when(annotation.targetIdArgument()).thenReturn(0);
        when(joinPoint.getArgs()).thenReturn(new Object[] { 42L });
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void successWritesExactlyOneRowForAuthenticatedPrincipal() throws Throwable {
        Object expected = new Object();
        when(joinPoint.proceed()).thenReturn(expected);

        assertThat(aspect.audit(joinPoint, annotation)).isSameAs(expected);

        ArgumentCaptor<AuditLog> captured = ArgumentCaptor.forClass(AuditLog.class);
        verify(writer, times(1)).write(captured.capture());
        verifyNoMoreInteractions(writer);
        assertThat(captured.getValue().getPrincipal()).isEqualTo("alice");
        assertThat(captured.getValue().getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(captured.getValue().getTargetId()).isEqualTo("42");
        assertThat(captured.getValue().getFailureReason()).isNull();
    }

    @Test
    void failureWritesReasonAndRethrowsOriginalException() throws Throwable {
        IllegalStateException failure = new IllegalStateException("section full");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.audit(joinPoint, annotation)).isSameAs(failure);

        ArgumentCaptor<AuditLog> captured = ArgumentCaptor.forClass(AuditLog.class);
        verify(writer, times(1)).write(captured.capture());
        assertThat(captured.getValue().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(captured.getValue().getFailureReason()).contains("section full");
        assertThat(captured.getValue().getPrincipal()).isEqualTo("alice");
    }
}
