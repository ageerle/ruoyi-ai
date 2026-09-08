package org.ruoyi.service.coding.harness.journal;

import java.nio.file.Path;

/** Immutable outcome returned after both checkpoint and index have been atomically published. */
public record JournalProjectionResult(Path checkpointFile, Path indexFile) {
}
