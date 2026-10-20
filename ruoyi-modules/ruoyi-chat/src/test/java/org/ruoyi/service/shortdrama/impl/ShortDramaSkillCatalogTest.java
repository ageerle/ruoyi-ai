package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaSkillCatalogTest {
    @TempDir Path root;
    private ShortDramaSkillCatalog catalog() { return new ShortDramaSkillCatalog(root.toString()); }
    private ShortDramaSkillCatalog.Save input(String name, String type, boolean enabled, String body, String expected) {
        return new ShortDramaSkillCatalog.Save(name, "测试技能", type, "明确身份与当前时点", enabled,
            "aesthetic".equals(type) ? "chinese-3d" : "", body,
            List.of(new ShortDramaSkillCatalog.SkillFile("references/source.md", "原创提炼"),
                    new ShortDramaSkillCatalog.SkillFile("scripts/review.py", "raise RuntimeError('MUST NEVER EXECUTE')")), expected);
    }
    @Test void optionsComeFromActualStoredSkillsAndInitializationNeverOverwritesAnEdit() {
        var catalog = catalog(); assertEquals(8, catalog.list("aesthetic", false).size());
        assertEquals(8, catalog.list("director", false).size());
        assertTrue(catalog.detail("cinematic-storyboard").files().stream().anyMatch(f -> f.path().equals("references/libtv-sources.md")));
        var created = catalog.save("test-art", input("test-art", "aesthetic", true, "原创审美方向甲", null), true);
        assertTrue(catalog.list("aesthetic", false).stream().anyMatch(s -> s.name().equals("test-art")));
        var updated = catalog.save("test-art", input("test-art", "aesthetic", true, "原创审美方向乙", created.version()), false);
        assertNotEquals(created.version(), updated.version());
        assertEquals("原创审美方向乙", new ShortDramaSkillCatalog(root.toString()).detail("test-art").body());
        assertEquals(2, updated.files().size());
        assertEquals("raise RuntimeError('MUST NEVER EXECUTE')", updated.files().stream().filter(f -> f.path().startsWith("scripts/")).findFirst().orElseThrow().content());
    }
    @Test void staleVersionsFailAndKeepTheNewerContent() {
        var catalog = catalog();
        var initial = catalog.save("test-director", input("test-director", "director", true, "节奏甲", null), true);
        var current = catalog.save("test-director", input("test-director", "director", true, "节奏乙", initial.version()), false);
        var error = assertThrows(IllegalStateException.class, () -> catalog.save("test-director", input("test-director", "director", true, "误覆盖", initial.version()), false));
        assertTrue(error.getMessage().contains("版本冲突"));
        assertEquals(current.version(), catalog.detail("test-director").version());
        assertEquals("节奏乙", catalog.detail("test-director").body());
    }
    @Test void eachSupplementaryCategoryAllowsOneSkillAndPrimaryRolesHaveSeparateFields() {
        var catalog = catalog();
        catalog.save("test-writing-a", input("test-writing-a", "screenwriting", true, "人物甲", null), true);
        catalog.save("test-writing", input("test-writing", "screenwriting", true, "人物诉求", null), true);
        assertEquals(List.of("test-writing"), catalog.normalizeStoryboardSkills(List.of("test-writing", "test-writing")));
        assertEquals(List.of(), catalog.normalizeStoryboardSkills(List.of()));
        assertNull(catalog.normalizeStoryboardSkills(null));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(List.of("test-writing-a", "test-writing"))).getMessage().contains("最多选择一个"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(List.of("chinese-3d-aesthetics"))).getMessage().contains("视觉风格"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(List.of("compact-short-drama"))).getMessage().contains("导演风格"));
        assertTrue(catalog.categories().stream().filter(ShortDramaSkillCatalog.Category::editable).allMatch(ShortDramaSkillCatalog.Category::projectSelectable));
    }
    @Test void olderCategoryFilesReadUnderCreativeRolesWithoutOverwritingAuthoredContents() throws Exception {
        var catalog = catalog();
        for (String oldType : List.of("workflow", "storyboard", "video", "reference")) {
            String name = "legacy-" + oldType;
            String role = "workflow".equals(oldType) ? "screenwriting" : "director";
            catalog.save(name, input(name, role, true, "用户维护正文", null), true);
            Path manifest = root.resolve(name + "/manifest.json");
            String oldManifest = Files.readString(manifest).replace("\"type\" : \"" + role + "\"", "\"type\" : \"" + oldType + "\"");
            Files.writeString(manifest, oldManifest);
            assertEquals(role, catalog.detail(name).type());
            assertEquals("用户维护正文", catalog.detail(name).body());
            assertEquals(oldManifest, Files.readString(manifest));
        }
        assertEquals("aesthetic", ShortDramaSkillCatalog.canonicalType("github-higgsfield-style-library", "reference"));
    }

    @Test void explicitAssetStyleTakesPrecedenceOverHistoricalSupplementalVisuals() {
        var catalog = catalog();
        var visual = catalog.save("legacy-visual", input("legacy-visual", "aesthetic", true, "过往视觉方向", null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("chinese-3d-aesthetics");
        project.setStoryboardSkillNames(List.of(visual.name()));
        assertTrue(catalog.selectedVisual(project, "aesthetic").contains("[selected-skill:chinese-3d-aesthetics@"));
        assertFalse(catalog.selectedVisual(project, "aesthetic").contains(visual.body()));
        assertEquals(List.of(visual.name()), project.getStoryboardSkillNames());
    }

    @Test void disabledAndMissingBindingsAreReadableButCannotSilentlyGenerateOrReuseCache() {
        var catalog = catalog(); var project = new ShortDramaProject();
        project.setAestheticSkillName("chinese-3d-aesthetics"); project.setDirectorSkillName("compact-short-drama");
        String version = catalog.projectVersion(project);
        var current = catalog.detail("compact-short-drama");
        assertTrue(catalog.selected(project, "director").contains(current.version()));
        catalog.save("compact-short-drama", new ShortDramaSkillCatalog.Save(current.name(), current.title(), current.type(), current.description(),
            false, current.artStyle(), current.body(), current.files(), current.version()), false);
        assertFalse(catalog.detail("compact-short-drama").enabled());
        assertTrue(catalog.list("director", false).stream().noneMatch(s -> s.name().equals("compact-short-drama")));
        assertEquals(8, catalog.list("director", true).size());
        assertThrows(IllegalArgumentException.class, () -> catalog.projectVersion(project));
        project.setDirectorSkillName("missing-director");
        assertThrows(IllegalArgumentException.class, () -> catalog.projectVersion(project));
        project.setDirectorSkillName(""); assertNotEquals(version, catalog.projectVersion(project));
        project.setAestheticSkillName(""); assertEquals("", catalog.selected(project, "aesthetic"));
    }
    @Test void fileAndNameTraversalAreRejectedBeforeWriting() {
        for (String name : List.of("../escape", "C:/escape", "bad/name", "_hidden", "Uppercase"))
            assertThrows(IllegalArgumentException.class, () -> ShortDramaSkillCatalog.safeName(name));
        for (String path : List.of("../SKILL.md", "references/../../escape.md", "C:/escape.md", "references\\bad.md", "scripts//bad.py", "scripts/./bad.py", "references/page.html", "scripts/run.exe"))
            assertThrows(IllegalArgumentException.class, () -> ShortDramaSkillCatalog.safeFile(path));
        assertEquals("references/era/source.md", ShortDramaSkillCatalog.safeFile("references/era/source.md"));
        assertFalse(Files.exists(root.resolve("escape.md")));
    }
    @Test void oversizedOrMalformedUtf8FilesFailInsteadOfReplacingOrExecutingContent() throws Exception {
        var catalog = catalog();
        assertThrows(IllegalArgumentException.class, () -> catalog.save("large-art", input("large-art", "aesthetic", true, "字".repeat(100000), null), true));
        catalog.save("utf8-art", input("utf8-art", "aesthetic", true, "方向正文", null), true);
        Files.write(root.resolve("utf8-art/references/source.md"), new byte[]{(byte)0xc3, 0x28});
        assertThrows(IllegalStateException.class, () -> catalog.detail("utf8-art"));
    }
    @Test void selectedReferenceEditsChangePlanningSignatureWithoutChangingExistingAssetDescriptions() {
        var catalog = catalog(); var project = new ShortDramaProject(); project.setAestheticSkillName("chinese-3d-aesthetics");
        var skill = catalog.detail(project.getAestheticSkillName()); String version = catalog.projectVersion(project);
        var location = new org.ruoyi.domain.entity.shortdrama.ShortDramaLocation(); location.setId(1L); location.setName("小县衙"); location.setDescriptions("低旧土墙");
        var before = new ShortDramaSceneCheckpoint.Assets(List.of(), List.of(), List.of(location), version).signature();
        catalog.save(skill.name(), new ShortDramaSkillCatalog.Save(skill.name(), skill.title(), skill.type(), skill.description(), true,
            skill.artStyle(), skill.body(), List.of(new ShortDramaSkillCatalog.SkillFile("references/lighting.md", "新的材料观察")), skill.version()), false);
        var after = new ShortDramaSceneCheckpoint.Assets(List.of(), List.of(), List.of(location), catalog.projectVersion(project)).signature();
        assertNotEquals(before, after); assertEquals("低旧土墙", location.getDescriptions());
        assertTrue(catalog.selected(project, "aesthetic").contains("默认结构审阅规则优先"));
    }
    @Test void llmLoadsReferencesButMediaPromptsUseCuratedBodyAndScriptsNeverBecomeInstructions() {
        var catalog = catalog(); catalog.save("test-art", input("test-art", "aesthetic", true, "已审美术方向", null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("test-art");
        String llm = catalog.selected(project, "aesthetic"), visual = catalog.selectedVisual(project, "aesthetic");
        assertTrue(llm.contains("原创提炼")); assertTrue(llm.contains("scripts/review.py")); assertFalse(llm.contains("raise RuntimeError"));
        assertFalse(visual.contains("原创提炼")); assertFalse(visual.contains("raise RuntimeError")); assertTrue(visual.contains("已审美术方向"));
        assertTrue(visual.contains(catalog.detail("test-art").version()));
    }

    @Test void videoVersionsValidateEachActualBindingWithoutReturningSkillBodies() {
        var catalog = catalog();
        var art = catalog.save("test-art", input("test-art", "aesthetic", true, "水墨淡彩且允许渗墨", null), true);
        var director = catalog.save("test-director", input("test-director", "director", true, "克制表演与动作接镜", null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName(art.name()); project.setDirectorSkillName(director.name());
        var versions = catalog.selectedVersions(project);
        assertEquals(art.name() + "@" + art.version(), versions.get("aesthetic"));
        assertEquals(director.name() + "@" + director.version(), versions.get("director"));
        assertFalse(versions.toString().contains(art.body()));
        catalog.save(director.name(), input(director.name(), "director", false, director.body(), director.version()), false);
        assertThrows(IllegalArgumentException.class, () -> catalog.selectedVersions(project));
    }
    @Test void existingMultiSkillBindingsRemainReadableWithoutWeakeningNewSelectionRules() {
        var catalog = catalog();
        var art = catalog.save("legacy-art", input("legacy-art", "aesthetic", true, "旧项目材料方向", null), true);
        var director = catalog.save("legacy-director", input("legacy-director", "director", true, "旧项目动作方向", null), true);
        var writer1 = catalog.save("legacy-writer-one", input("legacy-writer-one", "screenwriting", true, "因果写作方向", null), true);
        var writer2 = catalog.save("legacy-writer-two", input("legacy-writer-two", "screenwriting", true, "改编写作方向", null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("chinese-3d-aesthetics"); project.setDirectorSkillName("github-seedance-directing");
        var bindings = List.of(art.name(), director.name(), writer1.name(), writer2.name()); project.setStoryboardSkillNames(bindings);
        var versions = catalog.selectedVersions(project);
        for (String name : bindings) assertTrue(versions.containsKey("storyboard:" + name));
        assertTrue(catalog.selectedWritingDirection(project).contains("因果写作方向"));
        assertTrue(catalog.selectedWritingDirection(project).contains("改编写作方向"));
        assertFalse(catalog.selectedDirection(project).contains("因果写作方向"));
        assertFalse(catalog.selectedWritingDirection(project).contains("旧项目动作方向"));
        assertEquals(bindings, project.getStoryboardSkillNames());
        assertThrows(IllegalArgumentException.class, () -> catalog.normalizeStoryboardSkills(bindings));
        catalog.save(writer2.name(), input(writer2.name(), "screenwriting", false, writer2.body(), writer2.version()), false);
        assertThrows(IllegalArgumentException.class, () -> catalog.selectedVersions(project));
    }
    @Test void longSkillHandbooksDoNotFloodEachMediaPrompt() {
        var catalog = catalog();
        String handbook = "完整审美资料".repeat(500);
        catalog.save("long-art", input("long-art", "aesthetic", true, handbook, null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("long-art");
        String visual = catalog.selectedVisual(project, "aesthetic");
        assertTrue(visual.contains("明确身份与当前时点"));
        assertTrue(visual.contains("media-direction-summary"));
        assertFalse(visual.contains(handbook));
        assertTrue(visual.length() < 1500);
    }
    @Test void longHumanHandbookCannotOverrideAnExplicitMiniatureAvatarRatioInMedia() {
        var catalog = catalog();
        String handbook = "成年人采用7.5至8头身与自然长四肢。".repeat(220);
        catalog.save("human-handbook", new ShortDramaSkillCatalog.Save("human-handbook", "人物审美", "aesthetic",
            "按当前资产身份和明确比例执行", true, "chinese-3d", handbook, List.of(), null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("human-handbook");
        String description = "Codex微型AI助手，18厘米，全身严格1:4.2头身，短躯干与紧凑四肢";
        String prompt = ShortDramaCharacterArtPrompt.reference(description, catalog.effectiveArtStyle(project))
            + catalog.selectedVisual(project, "aesthetic");
        assertTrue(prompt.contains("1:4.2头身"));
        assertFalse(prompt.contains("成年人采用7.5至8头身"));
        assertTrue(prompt.length() < 4000);
    }
    @Test void longSkillsCanDefineAShortDedicatedMediaSection() {
        var catalog = catalog();
        String handbook = "# 完整体系\n" + "历史与审阅资料".repeat(300)
            + "\n## 媒体提示词\n成年骨相，清楚五官，衣料与金属分层；保持本张实际身份。\n## 来源\n馆藏登记";
        catalog.save("section-art", input("section-art", "aesthetic", true, handbook, null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("section-art");
        String visual = catalog.selectedVisual(project, "aesthetic");
        assertTrue(visual.contains("成年骨相，清楚五官"));
        assertFalse(visual.contains("历史与审阅资料历史与审阅资料"));
        assertFalse(visual.contains("馆藏登记"));
    }
    @Test void planningSnapshotCannotMixEditsOrChangedBaseStylesIntoASuccessfulRun() {
        var catalog = catalog(); var project = new ShortDramaProject(); project.setId(7L); project.setAestheticSkillName("chinese-3d-aesthetics"); project.setArtStyle("chinese-3d");
        var snapshot = catalog.snapshot(project); catalog.verify(snapshot, project);
        project.setArtStyle("realistic"); assertThrows(IllegalStateException.class, () -> catalog.verify(snapshot, project));
        project.setArtStyle("chinese-3d"); var current = catalog.detail("chinese-3d-aesthetics");
        catalog.save(current.name(), new ShortDramaSkillCatalog.Save(current.name(), current.title(), current.type(), current.description(), true,
            current.artStyle(), current.body() + "\n新版轮廓方向", current.files(), current.version()), false);
        assertThrows(IllegalStateException.class, () -> catalog.verify(snapshot, project));
        assertFalse(snapshot.direction().contains("新版轮廓方向"));
    }
    @Test void customAestheticCanDefineANewStyleWithoutInheritingTheLegacyPhotographicPrefix() {
        var catalog = catalog();
        catalog.save("ink-art", new ShortDramaSkillCatalog.Save("ink-art", "水墨木刻", "aesthetic", "以正文定义新的画风", true, "",
            "水墨木刻，纸纤维与留白，以疏密线条塑造衣物", List.of(), null), true);
        var project = new ShortDramaProject(); project.setArtStyle("realistic"); project.setAestheticSkillName("ink-art");
        assertEquals("custom-skill", catalog.effectiveArtStyle(project));
        String prompt = ShortDramaCharacterArtPrompt.reference("单幅人物定妆图，成年书生", catalog.effectiveArtStyle(project)) + catalog.selectedVisual(project, "aesthetic");
        assertTrue(prompt.contains("水墨木刻")); assertFalse(prompt.contains("【写实身份参照材质】"));
        project.setAestheticSkillName(""); assertEquals("realistic", catalog.effectiveArtStyle(project));
    }
    @Test void excessiveReferenceContextFailsBeforeLlmUseWhileMediaStillReceivesOnlyTheCuratedDirection() {
        var catalog = catalog(); catalog.save("large-reference", new ShortDramaSkillCatalog.Save("large-reference", "较长引用", "aesthetic", "受控模型资料预算", true,
            "chinese-3d", "简明美术方向", List.of(new ShortDramaSkillCatalog.SkillFile("references/book.md", "a".repeat(20001))), null), true);
        var project = new ShortDramaProject(); project.setAestheticSkillName("large-reference");
        assertThrows(IllegalArgumentException.class, () -> catalog.selected(project, "aesthetic"));
        assertTrue(catalog.selectedVisual(project, "aesthetic").contains("简明美术方向"));
    }
}
