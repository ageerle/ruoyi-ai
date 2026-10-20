package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaScriptPreparationTest {
    @Test void emptyLegacyWorldbuildingAddsNoContext() {
        assertEquals("", ShortDramaScriptPreparation.worldContext(new ShortDramaScript()));
    }

    @Test void savedProductionScopeReachesPlanningWithoutMutatingScript() {
        var script = new ShortDramaScript();
        script.setScriptText("阿宁：东西交给我。");
        script.setRevisionNotes("仅重写分镜；沿用已批准形象0，重设计候选不选用。");
        String context = ShortDramaScriptPreparation.worldContext(script);
        assertTrue(context.contains(script.getRevisionNotes()));
        assertEquals("阿宁：东西交给我。", script.getScriptText());
        script.setRevisionNotes(" ");
        assertEquals("", ShortDramaScriptPreparation.worldContext(script));
    }

    @Test void markdownMarkersAreRemovedFromScriptBody() {
        String text = ShortDramaServiceImpl.normalizePlainScriptText("""
            # 《青芜第一炮》
            ### 一　县衙。冬夜。
            **周慎（画外）**：大人！
            > 【行动】房门被撞开。
            """);
        assertEquals("""
            《青芜第一炮》

            一　县衙。冬夜。

            周慎（画外）：大人！

            【行动】房门被撞开。""", text);
    }

    @Test void formattingIsIdempotentAndPreservesDialogueAndStoryTime() {
        String source = "《青芜长明》第一集：第一炮\r\n一　旧库房。崇祯十一年冬。\r\n周先进入暗库。\r\n朱承晏（低声）：我先看看。\r\n三个月后，车架完成。";
        String formatted = ShortDramaServiceImpl.normalizePlainScriptText(source);
        assertEquals(source.replaceAll("\\s", ""), formatted.replaceAll("\\s", ""));
        assertTrue(formatted.contains("暗库。\n\n朱承晏"));
        assertEquals(formatted, ShortDramaServiceImpl.normalizePlainScriptText(formatted));
    }

    @Test void generatedScreenplaysNeverAssumeATargetDurationOrRewriteProducedScenes() {
        String prompt = ShortDramaServiceImpl.systemPromptCatalog().get("剧本写作与打磨");
        assertTrue(prompt.contains("除非用户本轮明确指定目标时长"));
        assertTrue(prompt.contains("不从旧剧本、参考案例或历史生成参数继承片长"));
        assertTrue(prompt.contains("只输出尚未生成的部分"));
        assertTrue(prompt.contains("段落之间空一行"));
    }
}
