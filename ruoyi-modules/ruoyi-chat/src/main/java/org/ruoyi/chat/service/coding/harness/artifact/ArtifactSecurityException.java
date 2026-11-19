package org.ruoyi.chat.service.coding.harness.artifact;

/** Raised when an artifact path is unsafe or escapes its configured root. */
public class ArtifactSecurityException extends ArtifactStoreException {

    public ArtifactSecurityException(String message) {
        super(message);
    }

    public ArtifactSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
