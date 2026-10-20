package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.domain.bo.shortdrama.ShortDramaIdeaBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptResult;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaScriptSkillsTest {
    @TempDir Path root;
    @Test void rewritePromptLoadsSelectedWritingBodiesAndReferencesButKeepsDownstreamSyntaxOut() {
        var catalog = new ShortDramaSkillCatalog(root.toString());
        catalog.save("test-writer", new ShortDramaSkillCatalog.Save("test-writer", "编剧", "screenwriting", "人物选择", true,
            "", "每场由人物选择改变处境", List.of(new ShortDramaSkillCatalog.SkillFile("references/dialogue.md", "对白试探而非说明")), null), true);
        catalog.save("test-video", new ShortDramaSkillCatalog.Save("test-video", "导演", "director", "参考语法", true,
            "", "仅视频阶段的供应商引用语法", List.of(), null), true);
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "skillCatalog", catalog);
        var bo = new ShortDramaIdeaBo(); bo.setProjectName("青芜长明"); bo.setIdea("原剧本");
        bo.setAestheticSkillName("chinese-3d-aesthetics"); bo.setDirectorSkillName("compact-short-drama");
        bo.setStoryboardSkillNames(List.of("test-writer", "test-video"));
        String prompt = ReflectionTestUtils.invokeMethod(service, "phase1UserPrompt", bo, "重写对白并让主角做选择");
        assertTrue(prompt.contains("每场由人物选择改变处境"));
        assertTrue(prompt.contains("对白试探而非说明"));
        assertTrue(prompt.contains(catalog.detail("test-writer").version()));
        assertFalse(prompt.contains("国风三维资产审美"));
        assertFalse(prompt.contains("[selected-skill:compact-short-drama@"));
        assertTrue(prompt.contains("重写对白并让主角做选择"));
        assertTrue(prompt.contains("[skill:story-causality@"));
        assertTrue(prompt.contains("[skill:emotional-dialogue@"));
        assertTrue(prompt.contains("[skill:github-story-methods@"));
        assertFalse(prompt.contains("仅视频阶段的供应商引用语法"));
        assertTrue(prompt.contains("正文不写镜号"));
        assertThrows(IllegalArgumentException.class, () -> {
            bo.setStoryboardSkillNames(List.of("missing-writer"));
            ReflectionTestUtils.invokeMethod(service, "phase1UserPrompt", bo, "改写");
        });
    }
    @Test void rewrittenScriptHasANewIdentityAndKeepsEarlierProducedSourcesIntact() {
        var old = new ShortDramaScript(); old.setId(42L); old.setProjectId(7L);
        old.setScriptText("原台词及已制镜头来源"); old.setTone("原基调");
        old.setWorldbuilding("同一年代与人物事实"); old.setSourceMaterials("原著全文"); old.setCreationMode("standard");
        var result = new ShortDramaScriptResult(); result.setScriptName("新稿"); result.setScriptText("新的选择与对白");
        result.setOutlineText("新的因果大纲"); result.setTone("三维国漫");
        var revised = ShortDramaServiceImpl.rewrittenScript(7L, old, result, "按技能重写", "真实模型", "技能版本");
        assertNotEquals(old.getId(), revised.getId()); assertEquals(7L, revised.getProjectId());
        assertEquals("原台词及已制镜头来源", old.getScriptText()); assertEquals("原基调", old.getTone());
        assertEquals("新的选择与对白", revised.getScriptText());
        assertEquals(old.getWorldbuilding(), revised.getWorldbuilding());
        assertEquals(old.getSourceMaterials(), revised.getSourceMaterials());
        assertTrue(revised.getRevisionNotes().contains("[writing-model:真实模型]"));
        assertTrue(revised.getRevisionNotes().contains("[selected-skills-version:技能版本]"));
    }
}
