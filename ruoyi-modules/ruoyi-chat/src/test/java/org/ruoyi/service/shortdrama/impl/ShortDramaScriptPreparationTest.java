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
}
