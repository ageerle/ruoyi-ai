package org.ruoyi.chat.service.coding.harness.tool;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Optional task contract restricting workspace mutations and named operations. */
public record ToolPolicyContract(
    List<String> allowedWriteRoots,
    Set<String> forbiddenOperations
) {

    public ToolPolicyContract {
        allowedWriteRoots = normalizeRoots(allowedWriteRoots);
        forbiddenOperations = normalizeOperations(forbiddenOperations);
    }

    public boolean forbids(ToolInvocation invocation) {
        String toolName = normalizeOperation(invocation.toolName());
        String operation = normalizeOperation(invocation.operation());
        return forbiddenOperations.contains("*")
            || forbiddenOperations.contains(toolName)
            || forbiddenOperations.contains(operation);
    }

    public boolean permitsAllMutationTargets(List<String> targets) {
        if (targets == null || targets.isEmpty() || allowedWriteRoots.isEmpty()) {
            return false;
        }
        for (String target : targets) {
            Path normalizedTarget;
            try {
                normalizedTarget = Path.of(target).normalize();
            } catch (InvalidPathException e) {
                return false;
            }
            if (!normalizedTarget.isAbsolute()) {
                return false;
            }
            boolean insideAllowedRoot = allowedWriteRoots.stream()
                .map(Path::of)
                .anyMatch(normalizedTarget::startsWith);
            if (!insideAllowedRoot) {
                return false;
            }
        }
        return true;
    }

    private static List<String> normalizeRoots(List<String> roots) {
        if (roots == null) {
            return List.of();
        }
        return roots.stream().map(root -> {
            if (root == null || root.isBlank()) {
                throw new IllegalArgumentException("Allowed write root cannot be blank");
            }
            try {
                Path path = Path.of(root).normalize();
                if (!path.isAbsolute()) {
                    throw new IllegalArgumentException("Allowed write root must be absolute: " + root);
                }
                return path.toString();
            } catch (InvalidPathException e) {
                throw new IllegalArgumentException("Invalid allowed write root: " + root, e);
            }
        }).distinct().toList();
    }

    private static Set<String> normalizeOperations(Set<String> operations) {
        if (operations == null || operations.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String operation : operations) {
            if (operation == null || operation.isBlank()) {
                throw new IllegalArgumentException("Forbidden operation cannot be blank");
            }
            normalized.add(normalizeOperation(operation));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizeOperation(String operation) {
        return operation.trim().toLowerCase(Locale.ROOT);
    }
}
