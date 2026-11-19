package org.ruoyi.chat.service.coding.harness.skill;

import dev.langchain4j.skills.ClassPathSkillLoader;
import dev.langchain4j.skills.Skill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HarnessSkillCatalogFactory {

    private static final String BUNDLED_ROOT = "coding-harness/skills";
    private static final int MAX_SKILLS = 100;
    private static final int MAX_DEPTH = 6;
    private static final long MAX_SKILL_BYTES = 256 * 1024;
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
        ".git", ".idea", "node_modules", "target", "dist", "build");

    public HarnessSkillCatalog load(Path workspace) {
        LinkedHashMap<String, HarnessSkillDefinition> skills = new LinkedHashMap<>();
        loadBundled(skills);
        for (String root : List.of(".agents/skills", ".claude/skills", ".codex/skills")) {
            if (skills.size() >= MAX_SKILLS) {
                break;
            }
            loadWorkspaceRoot(workspace.resolve(root), "workspace:" + root, skills);
        }
        return new HarnessSkillCatalog(List.copyOf(skills.values()));
    }

    private void loadBundled(Map<String, HarnessSkillDefinition> destination) {
        try {
            for (Skill skill : ClassPathSkillLoader.loadSkills(BUNDLED_ROOT)) {
                Map<String, String> resources = new LinkedHashMap<>();
                skill.resources().forEach(resource -> resources.put(
                    resource.relativePath().replace('\\', '/'), resource.content()));
                add(destination, new HarnessSkillDefinition(skill.name(), skill.description(),
                    skill.content(), "classpath", null, resources));
            }
        } catch (RuntimeException error) {
            log.warn("Unable to load bundled Harness skills from {}", BUNDLED_ROOT, error);
        }
    }

    private void loadWorkspaceRoot(Path root, String source,
                                   Map<String, HarnessSkillDefinition> destination) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        List<Path> manifests = discoverManifests(root);
        manifests.sort(Comparator.comparing(path -> root.relativize(path).toString()));
        Set<Path> selectedDirectories = new HashSet<>();
        for (Path manifest : manifests) {
            if (destination.size() >= MAX_SKILLS) {
                log.warn("Harness skill limit {} reached; remaining workspace skills are ignored", MAX_SKILLS);
                break;
            }
            Path directory = manifest.getParent();
            boolean nestedBelowSkill = selectedDirectories.stream().anyMatch(directory::startsWith);
            if (nestedBelowSkill) {
                continue;
            }
            try {
                if (Files.size(manifest) > MAX_SKILL_BYTES) {
                    log.warn("Skipping oversized Harness skill manifest {}", manifest);
                    continue;
                }
                String raw = Files.readString(manifest, StandardCharsets.UTF_8);
                FrontMatter parsed = parse(raw, directory.getFileName().toString());
                add(destination, new HarnessSkillDefinition(parsed.name(), parsed.description(),
                    parsed.body(), source, directory, Map.of()));
                selectedDirectories.add(directory);
            } catch (RuntimeException | IOException error) {
                log.warn("Skipping invalid Harness skill {}: {}", manifest, error.getMessage());
            }
        }
    }

    private List<Path> discoverManifests(Path root) {
        List<Path> manifests = new ArrayList<>();
        try {
            Files.walkFileTree(root, Set.of(), MAX_DEPTH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) {
                    if (!directory.equals(root)
                        && (attrs.isSymbolicLink()
                        || SKIPPED_DIRECTORIES.contains(directory.getFileName().toString()))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !attrs.isSymbolicLink()
                        && file.getFileName().toString().equals("SKILL.md")) {
                        manifests.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException error) {
            log.warn("Unable to discover Harness skills under {}", root, error);
        }
        return manifests;
    }

    private void add(Map<String, HarnessSkillDefinition> destination, HarnessSkillDefinition skill) {
        String name = skill.name().trim().toLowerCase();
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid skill name: " + skill.name());
        }
        if (skill.description().length() > 1_000 || skill.content().length() > MAX_SKILL_BYTES) {
            throw new IllegalArgumentException("Skill metadata or body is too large: " + name);
        }
        HarnessSkillDefinition normalized = new HarnessSkillDefinition(name,
            skill.description().strip(), skill.content(), skill.source(), skill.basePath(),
            skill.bundledResources());
        HarnessSkillDefinition conflict = destination.putIfAbsent(name, normalized);
        if (conflict != null) {
            log.warn("Ignoring duplicate Harness skill {} from {}; first source {} wins",
                name, skill.source(), conflict.source());
        }
    }

    private FrontMatter parse(String raw, String fallbackName) {
        String normalized = raw.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            throw new IllegalArgumentException("SKILL.md requires YAML frontmatter");
        }
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            throw new IllegalArgumentException("SKILL.md frontmatter is not closed");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String line : normalized.substring(4, end).split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                fields.put(line.substring(0, colon).trim(), unquote(line.substring(colon + 1).trim()));
            }
        }
        String name = fields.getOrDefault("name", fallbackName);
        String description = fields.get("description");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Skill description is required");
        }
        return new FrontMatter(name, description, normalized.substring(end + 5).strip());
    }

    private String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record FrontMatter(String name, String description, String body) { }
}
