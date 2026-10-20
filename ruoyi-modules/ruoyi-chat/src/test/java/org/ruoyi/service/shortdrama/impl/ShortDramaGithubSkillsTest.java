package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaGithubSkillsTest {
    @TempDir Path directory;

    @Test void sourcePackIsDiscoveredAndMarketExposesAllRealCategories() {
        var catalog = new ShortDramaSkillCatalog(directory.toString());
        var categories = catalog.categories();
        assertEquals(5, categories.size());
        assertEquals(3, categories.stream().filter(ShortDramaSkillCatalog.Category::editable).count());
        assertEquals(List.of("screenwriting", "director", "aesthetic"), categories.stream()
            .filter(ShortDramaSkillCatalog.Category::projectSelectable).map(ShortDramaSkillCatalog.Category::type).toList());
        var imported = catalog.list(null, true).stream().filter(s -> s.name().startsWith("github-")).toList();
        assertEquals(15, imported.size());
        var entries = new ShortDramaSkillMarket(catalog).list();
        for (var option : imported) {
            var skill = catalog.detail(option.name());
            assertTrue(categories.stream().anyMatch(c -> c.type().equals(skill.type())));
            assertTrue(entries.stream().anyMatch(e -> e.name().equals(skill.name()) && e.type().equals(skill.type())
                && e.version().equals(skill.version()) && e.body().equals(skill.body())));
            assertTrue(skill.body().contains("timing"));
            assertTrue(skill.files().stream().anyMatch(f -> f.path().equals("references/source.md")
                && f.content().contains("固定提交")));
        }
    }

    @Test void newCategoriesRoundTripAndVersionConflictsProtectRuntimeEdits() {
        var catalog = new ShortDramaSkillCatalog(directory.toString());
        for (String type : List.of("screenwriting", "director", "aesthetic")) {
            String name = "test-" + type;
            var initial = catalog.save(name, new ShortDramaSkillCatalog.Save(name, "方法", type, "阶段方法", true,
                "", "已有正文", List.of(), null), true);
            var changed = catalog.save(name, new ShortDramaSkillCatalog.Save(name, "方法", type, "阶段方法", false,
                "", "后台维护正文", List.of(), initial.version()), false);
            assertTrue(catalog.list(type, true).stream().anyMatch(s -> s.name().equals(name)));
            assertFalse(catalog.list(type, false).stream().anyMatch(s -> s.name().equals(name)));
            assertThrows(IllegalStateException.class, () -> catalog.save(name,
                new ShortDramaSkillCatalog.Save(name, "方法", type, "阶段方法", true, "", "过期内容", List.of(), initial.version()), false));
            assertEquals(changed.version(), new ShortDramaSkillCatalog(directory.toString()).detail(name).version());
        }
        var source = catalog.detail("github-novel-adaptation");
        var edited = catalog.save(source.name(), new ShortDramaSkillCatalog.Save(source.name(), source.title(), source.type(),
            source.description(), true, "", "用户已编辑的改编方法", source.files(), source.version()), false);
        assertEquals(edited.version(), new ShortDramaSkillCatalog(directory.toString()).detail(source.name()).version());
        var project = new ShortDramaProject();
        project.setDirectorSkillName(source.name());
        assertThrows(IllegalArgumentException.class, () -> catalog.selected(project, "director"));
        assertThrows(IllegalArgumentException.class, () -> catalog.list("system", true));
    }

    @Test void defaultMethodsFollowStagesAndDoNotLeakFullHandbooksIntoMedia() {
        String writing = ShortDramaDirectorSkills.load("story-causality");
        assertTrue(writing.contains("[skill:github-story-methods@"));
        assertFalse(writing.contains("[skill:github-shot-methods@"));
        String storyboard = ShortDramaDirectorSkills.load("cinematic-storyboard", "video-prompt", "director-review");
        assertEquals(1, storyboard.split("\\[skill:github-shot-methods@", -1).length - 1);
        assertTrue(storyboard.contains("[skill:github-production-methods@"));
        String media = ShortDramaDirectorSkills.media("cinematic-storyboard", "video-prompt");
        assertFalse(media.contains("github-shot-methods"));
        assertFalse(media.contains("[skill:github-production-methods@"));
    }

    @Test void creativeRolesAreSeparatedAndEachSelectedPackageInvalidatesFrozenPlans() {
        var catalog = new ShortDramaSkillCatalog(directory.toString());
        var project = new ShortDramaProject();
        project.setDirectorSkillName("github-seedance-directing");
        project.setStoryboardSkillNames(catalog.normalizeStoryboardSkills(List.of("github-short-drama-writing", "github-short-drama-writing")));
        assertEquals(1, project.getStoryboardSkillNames().size());
        String direction = catalog.selectedDirection(project);
        assertEquals(1, direction.split("\\[selected-skill:github-seedance-directing@", -1).length - 1);
        assertFalse(direction.contains("[selected-skill:github-short-drama-writing@"));
        assertTrue(catalog.selectedWritingDirection(project).contains("[selected-skill:github-short-drama-writing@"));
        assertFalse(catalog.selectedWritingDirection(project).contains("[selected-skill:github-seedance-directing@"));
        assertFalse(catalog.selectedVisual(project, "director").contains("固定提交"));
        assertFalse(catalog.selectedVisual(project, "director").contains("github-seedance-consistency@"));
        assertEquals(2, catalog.selectedVersions(project).size());
        var frozen = catalog.snapshot(project);
        var method = catalog.detail("github-short-drama-writing");
        catalog.save(method.name(), new ShortDramaSkillCatalog.Save(method.name(), method.title(), method.type(),
            method.description(), true, "", method.body() + "\n新的镜间接续规则", method.files(), method.version()), false);
        assertThrows(IllegalStateException.class, () -> catalog.verify(frozen, project));
        project.setStoryboardSkillNames(List.of());
        assertFalse(catalog.selectedDirection(project).contains("github-manju-directing@"));
        assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(List.of("missing-skill")));
        var disabled = catalog.detail("github-manju-directing");
        catalog.save(disabled.name(), new ShortDramaSkillCatalog.Save(disabled.name(), disabled.title(), disabled.type(),
            disabled.description(), false, "", disabled.body(), disabled.files(), disabled.version()), false);
        assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(List.of(disabled.name())));
    }
}
