package org.ruoyi.service.coding.harness.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Descriptive identity and role metadata for one project inside a session workspace lease. */
public record WorkspaceManifestProject(
    String projectId,
    String relativePath,
    String displayName,
    List<String> tags
) {

    static final int MAX_PROJECT_ID_LENGTH = 64;
    static final int MAX_RELATIVE_PATH_LENGTH = 512;
    static final int MAX_DISPLAY_NAME_LENGTH = 128;
    static final int MAX_TAGS = 16;
    static final int MAX_TAG_LENGTH = 64;
    private static final Pattern SAFE_PROJECT_ID =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public WorkspaceManifestProject {
        projectId = requireText(projectId, "projectId", MAX_PROJECT_ID_LENGTH);
        if (!SAFE_PROJECT_ID.matcher(projectId).matches()) {
            throw new IllegalArgumentException(
                "Workspace manifest projectId must be a safe identifier");
        }
        relativePath = requireText(relativePath, "relativePath", MAX_RELATIVE_PATH_LENGTH);
        displayName = requireText(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        tags = normalizeTags(tags);
    }

    /** Unknown fields such as writable/permissionMode must fail closed instead of being ignored. */
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException(
            "Unknown workspace manifest project property: " + property);
    }

    private static List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        if (rawTags.size() > MAX_TAGS) {
            throw new IllegalArgumentException(
                "Workspace manifest project accepts at most " + MAX_TAGS + " tags");
        }
        List<String> normalized = new ArrayList<>(rawTags.size());
        Set<String> identities = new HashSet<>();
        for (String rawTag : rawTags) {
            String tag = requireText(rawTag, "tag", MAX_TAG_LENGTH);
            if (!identities.add(tag.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                    "Workspace manifest project contains a duplicate tag: " + tag);
            }
            normalized.add(tag);
        }
        normalized.sort(Comparator.comparing((String value) -> value.toLowerCase(Locale.ROOT))
            .thenComparing(Comparator.naturalOrder()));
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "Workspace manifest project " + field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                "Workspace manifest project " + field + " exceeds " + maxLength
                    + " characters");
        }
        return normalized;
    }
}
