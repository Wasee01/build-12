package com.example.auditing.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Writes audit rows in an independent transaction, including on business rollback. */
@Component
public class AuditWriter {
    private final AuditLogRepository repository;
    private final TransactionTemplate requiresNew;

    public AuditWriter(AuditLogRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(manager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void write(AuditLog log) {
        requiresNew.executeWithoutResult(status -> repository.saveAndFlush(log));
    }
}
