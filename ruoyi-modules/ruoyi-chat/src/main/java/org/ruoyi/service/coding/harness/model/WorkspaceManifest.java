package org.ruoyi.service.coding.harness.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.List;

/** Immutable, versioned description of projects contained by one session workspace lease. */
public record WorkspaceManifest(
    int schemaVersion,
    List<WorkspaceManifestProject> projects
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_PROJECTS = 32;

    public WorkspaceManifest {
        // Jackson supplies zero when an older/unversioned request omitted this primitive field.
        schemaVersion = schemaVersion == 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                "Unsupported workspace manifest schema version: " + schemaVersion);
        }
        projects = projects == null ? legacyProjects() : List.copyOf(projects);
        if (projects.isEmpty() || projects.size() > MAX_PROJECTS
            || projects.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                "Workspace manifest requires 1 to " + MAX_PROJECTS + " projects");
        }
    }

    public WorkspaceManifest(List<WorkspaceManifestProject> projects) {
        this(CURRENT_SCHEMA_VERSION, projects);
    }

    /** Compatibility projection for old sessions and create requests without a manifest. */
    public static WorkspaceManifest legacySingleProject() {
        return new WorkspaceManifest(CURRENT_SCHEMA_VERSION, legacyProjects());
    }

    /** Unknown fields cannot be used to smuggle an authorization contract into the manifest. */
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown workspace manifest property: " + property);
    }

    private static List<WorkspaceManifestProject> legacyProjects() {
        return List.of(new WorkspaceManifestProject(
            "workspace", ".", "Workspace", List.of("primary")));
    }
}
