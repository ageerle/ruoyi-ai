package org.ruoyi.service.coding.harness.journal;

/** Projects an immutable context snapshot without taking ownership of run state. */
@FunctionalInterface
public interface RunJournalProjector {

    JournalProjectionResult project(StructuredContextSnapshot snapshot) throws JournalProjectionException;
}
