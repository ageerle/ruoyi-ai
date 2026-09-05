package org.ruoyi.ipd.service;

import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/** Record a preflight attempt or a failure after the business transaction has completed/rolled back. */
@Service
public class AuditAttemptService {
    public enum Outcome { ATTEMPT, FAILURE, DENIED }

    private final AuditLogService audit;
    private final TransactionTemplate independent;

    public AuditAttemptService(AuditLogService audit, PlatformTransactionManager transactions) {
        this.audit = audit;
        independent = new TransactionTemplate(transactions);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public AuditLog record(IpdActor actor, Outcome outcome, String attemptedAction,
                           String entityType, Long entityId, String reason) {
        // Suspending an outer transaction that already owns the chain head would self-deadlock.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Record independent audit events outside the business transaction");
        }
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(actor.id(), "actor.id");
        Objects.requireNonNull(outcome, "outcome");
        if (attemptedAction == null || attemptedAction.isBlank()) {
            throw new IllegalArgumentException("attemptedAction is required");
        }
        return independent.execute(status -> audit.append(AuditLog.builder()
            .operatorId(actor.id()).operatorName(actor.name()).operatorRole(actor.role())
            .action(outcome.name()).entityType(entityType).entityId(entityId).reason(reason)
            .afterData(AuditEventData.json("outcome", outcome.name(), "attemptedAction", attemptedAction))
            .build()));
    }
}
