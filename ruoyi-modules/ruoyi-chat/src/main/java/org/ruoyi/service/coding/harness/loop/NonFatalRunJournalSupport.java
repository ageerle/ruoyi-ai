package org.ruoyi.service.coding.harness.loop;

import org.ruoyi.service.coding.harness.journal.RunJournalProjector;
import org.ruoyi.service.coding.harness.journal.StructuredContextSnapshotFactory;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/** Optional, fail-open operator projection that never owns run state or control flow. */
final class NonFatalRunJournalSupport {

    private static final Logger LOG = LoggerFactory.getLogger(NonFatalRunJournalSupport.class);

    private final Supplier<RunJournalProjector> projectorSupplier;
    private final Supplier<StructuredContextSnapshotFactory> factorySupplier;

    NonFatalRunJournalSupport(Supplier<RunJournalProjector> projectorSupplier,
                              Supplier<StructuredContextSnapshotFactory> factorySupplier) {
        this.projectorSupplier = Objects.requireNonNull(projectorSupplier, "projectorSupplier");
        this.factorySupplier = Objects.requireNonNull(factorySupplier, "factorySupplier");
    }

    static NonFatalRunJournalSupport disabled() {
        return new NonFatalRunJournalSupport(() -> null, () -> null);
    }

    /**
     * Returns whether a projection was published. Disabled beans, invalid snapshots and I/O
     * failures all return false and are deliberately invisible to the durable run transition.
     */
    boolean projectBestEffort(HarnessRunState run, HarnessSessionState session) {
        try {
            RunJournalProjector projector = projectorSupplier.get();
            if (projector == null) {
                return false;
            }
            StructuredContextSnapshotFactory factory = factorySupplier.get();
            if (factory == null) {
                return false;
            }
            projector.project(factory.create(run, session));
            return true;
        } catch (Exception failure) {
            String runId = run == null ? "unknown" : run.runId();
            LOG.warn("Non-fatal Harness journal projection failed for run {}", runId, failure);
            return false;
        }
    }
}
