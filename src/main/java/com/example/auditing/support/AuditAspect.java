package com.example.auditing.support;

import java.time.Clock;
import java.time.Instant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
// Run after ExposeInvocationInterceptor so Spring can bind the annotation argument.
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AuditAspect {
    private final AuditWriter writer;
    private final Clock clock;

    @Autowired
    public AuditAspect(AuditWriter writer) {
        this(writer, Clock.systemUTC());
    }

    // Package-private clock injection makes the timestamp deterministic in unit tests.
    AuditAspect(AuditWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    @Around("@annotation(auditedOperation)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditedOperation auditedOperation) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null || !authentication.isAuthenticated()
                ? "anonymous" : authentication.getName();
        String targetId = targetId(joinPoint.getArgs(), auditedOperation.targetIdArgument());

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable original) {
            // Never roll back a failure audit with the failed business transaction.
            try {
                writer.write(new AuditLog(principal, auditedOperation.action(), auditedOperation.targetType(),
                        targetId, Instant.now(clock), AuditOutcome.FAILURE, reason(original)));
            } catch (RuntimeException auditFailure) {
                original.addSuppressed(auditFailure);
            }
            throw original;
        }
        // Audit persistence errors are not mislabeled as business failures.
        writer.write(new AuditLog(principal, auditedOperation.action(), auditedOperation.targetType(),
                targetId, Instant.now(clock), AuditOutcome.SUCCESS, null));
        return result;
    }

    private static String targetId(Object[] args, int index) {
        if (index < 0 || index >= args.length || args[index] == null) {
            return null;
        }
        return String.valueOf(args[index]);
    }

    private static String reason(Throwable throwable) {
        String message = throwable.getMessage();
        String value = throwable.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
