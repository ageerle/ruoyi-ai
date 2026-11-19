package org.ruoyi.chat.service.coding.harness.artifact;

/** Raised when stored bytes no longer match their content-addressed SHA-256 id. */
public class ArtifactCorruptedException extends ArtifactStoreException {

    public ArtifactCorruptedException(String expectedHash, String actualHash) {
        super("Artifact integrity check failed: expected=" + expectedHash + ", actual=" + actualHash);
    }
}
