package org.ruoyi.chat.service.coding.harness.artifact;

/** Raised before an oversized artifact or read can be accepted. */
public class ArtifactLimitExceededException extends ArtifactStoreException {

    public ArtifactLimitExceededException(String message) {
        super(message);
    }
}
