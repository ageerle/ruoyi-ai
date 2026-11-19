package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** Small, deterministic .gitignore subset suitable for traversal when git is unavailable. */
final class GitIgnoreMatcher {

    private static final Set<String> COMMON_DIRECTORIES = Set.of(
        ".git", ".hg", ".svn", ".idea", ".gradle", "node_modules", "target",
        "build", "dist", "coverage", ".next", ".nuxt"
    );

    private final Path root;
    private final Map<Path, List<Rule>> rulesByDirectory = new HashMap<>();

    GitIgnoreMatcher(Path root) {
        this.root = root;
    }

    boolean ignored(Path absolutePath, boolean directory) {
        String relative = slash(root.relativize(absolutePath));
        if (relative.isEmpty()) {
            return false;
        }
        for (Path segment : root.relativize(absolutePath)) {
            if (COMMON_DIRECTORIES.contains(segment.toString().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        boolean ignored = false;
        Path current = root;
        int directoryCount = Math.max(0, root.relativize(absolutePath).getNameCount() - 1);
        for (int depth = 0; depth <= directoryCount; depth++) {
            for (Rule rule : rules(current)) {
                if (rule.matches(relative, directory)) {
                    ignored = !rule.negated();
                }
            }
            if (depth < directoryCount) {
                current = current.resolve(root.relativize(absolutePath).getName(depth));
            }
        }
        return ignored;
    }

    private List<Rule> rules(Path directory) {
        return rulesByDirectory.computeIfAbsent(directory, this::loadRules);
    }

    private List<Rule> loadRules(Path directory) {
        Path ignoreFile = directory.resolve(".gitignore");
        if (!Files.isRegularFile(ignoreFile, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(ignoreFile)) {
            return List.of();
        }
        try {
            String base = slash(root.relativize(directory));
            List<Rule> rules = new ArrayList<>();
            for (String rawLine : Files.readAllLines(ignoreFile, StandardCharsets.UTF_8)) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                boolean negated = line.startsWith("!");
                if (negated) {
                    line = line.substring(1);
                }
                boolean directoryOnly = line.endsWith("/");
                if (directoryOnly) {
                    line = line.substring(0, line.length() - 1);
                }
                boolean anchored = line.startsWith("/");
                if (anchored) {
                    line = line.substring(1);
                }
                if (!line.isBlank()) {
                    try {
                        rules.add(new Rule(base, line, negated, directoryOnly, anchored,
                            PathGlob.compile(line, anchored)));
                    } catch (RuntimeException invalidPattern) {
                        // Match git's tolerant behavior: one malformed rule must not break traversal.
                    }
                }
            }
            return List.copyOf(rules);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }

    private record Rule(
        String base,
        String expression,
        boolean negated,
        boolean directoryOnly,
        boolean anchored,
        PathGlob matcher
    ) {
        boolean matches(String rootRelative, boolean directory) {
            String candidate;
            if (base.isEmpty()) {
                candidate = rootRelative;
            } else if (rootRelative.equals(base)) {
                candidate = "";
            } else if (rootRelative.startsWith(base + "/")) {
                candidate = rootRelative.substring(base.length() + 1);
            } else {
                return false;
            }

            if (directoryOnly) {
                if (anchored || expression.contains("/")) {
                    return candidate.equals(expression) || candidate.startsWith(expression + "/")
                        || matcher.matches(candidate);
                }
                for (String segment : candidate.split("/")) {
                    if (matcher.matches(segment)) {
                        return true;
                    }
                }
                return false;
            }
            if (!directory && matcher.matches(candidate)) {
                return true;
            }
            if (!anchored && !expression.contains("/")) {
                for (String segment : candidate.split("/")) {
                    if (matcher.matches(segment)) {
                        return true;
                    }
                }
            }
            return matcher.matches(candidate);
        }
    }
}
