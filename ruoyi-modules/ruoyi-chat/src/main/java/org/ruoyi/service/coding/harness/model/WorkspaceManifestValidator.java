package org.ruoyi.service.coding.harness.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves a requested manifest against the canonical session workspace lease. */
public final class WorkspaceManifestValidator {

    public WorkspaceManifest normalize(Path leaseRoot, WorkspaceManifest requestedManifest) {
        if (leaseRoot == null) {
            throw new IllegalArgumentException("Workspace lease root is required");
        }
        WorkspaceManifest manifest = requestedManifest == null
            ? WorkspaceManifest.legacySingleProject() : requestedManifest;
        Path canonicalRoot = canonicalDirectory(leaseRoot, "Workspace lease root");
        Set<String> projectIds = new HashSet<>();
        Set<String> projectPaths = new HashSet<>();
        List<WorkspaceManifestProject> normalized = new ArrayList<>(manifest.projects().size());
        for (WorkspaceManifestProject project : manifest.projects()) {
            String projectIdentity = project.projectId().toLowerCase(Locale.ROOT);
            if (!projectIds.add(projectIdentity)) {
                throw new IllegalArgumentException(
                    "Workspace manifest contains duplicate projectId: " + project.projectId());
            }
            Path relative = parseRelativePath(project.relativePath());
            Path lexicalTarget = canonicalRoot.resolve(relative).normalize();
            if (!lexicalTarget.startsWith(canonicalRoot)) {
                throw new IllegalArgumentException(
                    "Workspace manifest project escaped the session workspace: "
                        + project.projectId());
            }
            if (!Files.isDirectory(lexicalTarget, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException(
                    "Workspace manifest project directory does not exist: "
                        + project.relativePath());
            }
            Path canonicalProject = canonicalDirectory(lexicalTarget,
                "Workspace manifest project " + project.projectId());
            if (!canonicalProject.startsWith(canonicalRoot)) {
                throw new IllegalArgumentException(
                    "Workspace manifest project resolves outside the session workspace: "
                        + project.projectId());
            }
            String pathIdentity = canonicalProject.toString().toLowerCase(Locale.ROOT);
            if (!projectPaths.add(pathIdentity)) {
                throw new IllegalArgumentException(
                    "Workspace manifest contains duplicate project path: "
                        + project.relativePath());
            }
            Path normalizedRelative = canonicalRoot.relativize(canonicalProject);
            String normalizedPath = normalizedRelative.toString().isEmpty()
                ? "." : normalizedRelative.toString().replace('\\', '/');
            normalized.add(new WorkspaceManifestProject(project.projectId(), normalizedPath,
                project.displayName(), project.tags()));
        }
        return new WorkspaceManifest(WorkspaceManifest.CURRENT_SCHEMA_VERSION, normalized);
    }

    private Path parseRelativePath(String value) {
        final Path relative;
        try {
            relative = Path.of(value);
        } catch (InvalidPathException invalid) {
            throw new IllegalArgumentException(
                "Workspace manifest project path is invalid: " + value, invalid);
        }
        if (relative.isAbsolute() || relative.getRoot() != null) {
            throw new IllegalArgumentException(
                "Workspace manifest project path must be relative: " + value);
        }
        for (Path segment : relative) {
            if ("..".equals(segment.toString())) {
                throw new IllegalArgumentException(
                    "Workspace manifest project path cannot contain '..': " + value);
            }
        }
        return relative;
    }

    private Path canonicalDirectory(Path path, String label) {
        try {
            Path canonical = path.toRealPath().normalize();
            if (!Files.isDirectory(canonical, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException(label + " is not a directory: " + path);
            }
            return canonical;
        } catch (IOException error) {
            throw new IllegalArgumentException(label + " cannot be resolved: " + path, error);
        }
    }
}
