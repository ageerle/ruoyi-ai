package org.ruoyi.chat.service.coding.harness.artifact;

/** Raised when an artifact cannot be durably committed. */
public class ArtifactWriteException extends ArtifactStoreException {

    public ArtifactWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
