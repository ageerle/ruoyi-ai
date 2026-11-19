package org.ruoyi.chat.service.coding.harness.artifact;

/** Raised when a valid scoped artifact id has no stored object. */
public class ArtifactNotFoundException extends ArtifactStoreException {

    public ArtifactNotFoundException(String artifactId) {
        super("Artifact was not found: " + artifactId);
    }
}
