package org.ruoyi.chat.service.coding.harness.skill;

import org.ruoyi.chat.service.coding.WorkspaceGuard;
import org.ruoyi.chat.service.coding.harness.prompt.HarnessSkillMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable per-run catalog. Full skill bodies/resources are disclosed only on explicit activation. */
public final class HarnessSkillCatalog {

    private static final long MAX_RESOURCE_BYTES = 1024 * 1024;
    private final Map<String, HarnessSkillDefinition> definitions;

    HarnessSkillCatalog(List<HarnessSkillDefinition> skills) {
        Map<String, HarnessSkillDefinition> indexed = new LinkedHashMap<>();
        for (HarnessSkillDefinition skill : skills) {
            indexed.putIfAbsent(normalize(skill.name()), skill);
        }
        definitions = Map.copyOf(indexed);
    }

    public List<HarnessSkillMetadata> metadata() {
        return definitions.values().stream()
            .sorted(Comparator.comparing(HarnessSkillDefinition::name))
            .map(skill -> new HarnessSkillMetadata(skill.name(), skill.description(), skill.source()))
            .toList();
    }

    public HarnessSkillDefinition activate(String name) {
        HarnessSkillDefinition skill = definitions.get(normalize(name));
        if (skill == null) {
            throw new IllegalArgumentException("Unknown skill: " + name + ". Available: "
                + definitions.values().stream().map(HarnessSkillDefinition::name).sorted().toList());
        }
        return skill;
    }

    public String readResource(String skillName, String relativePath) {
        HarnessSkillDefinition skill = activate(skillName);
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Skill resource path is required");
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")) {
            throw new IllegalArgumentException("Skill resource must be a relative path");
        }
        String bundled = skill.bundledResources().get(normalized);
        if (bundled != null) {
            return bundled;
        }
        if (skill.basePath() == null) {
            throw new IllegalArgumentException("Unknown resource for skill " + skillName + ": " + relativePath);
        }
        try {
            Path base = skill.basePath().toRealPath();
            Path candidate = base.resolve(relativePath).normalize();
            if (!WorkspaceGuard.isWithinWorkspace(base, candidate)
                || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Unknown or unsafe skill resource: " + relativePath);
            }
            if (Files.size(candidate) > MAX_RESOURCE_BYTES) {
                throw new IllegalArgumentException("Skill resource exceeds 1 MB: " + relativePath);
            }
            return Files.readString(candidate, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot read skill resource: " + relativePath, error);
        }
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
