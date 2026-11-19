package org.ruoyi.chat.service.coding.harness.artifact;

/** Base exception for artifact persistence and integrity failures. */
public class ArtifactStoreException extends RuntimeException {

    public ArtifactStoreException(String message) {
        super(message);
    }

    public ArtifactStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
