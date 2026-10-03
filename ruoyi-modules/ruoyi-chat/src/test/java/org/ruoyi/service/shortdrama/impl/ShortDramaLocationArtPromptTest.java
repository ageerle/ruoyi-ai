package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaLocationArtPromptTest {
    @Test void requestOnlyMorningAndPropCleanupOverrideRejectedReferenceWithoutChangingFrozenGeometry() {
        String name = "驿道_秋日傍晚", description = "旧石桥与土路，路旁木架，傍晚暖金色光";
        String requirements = "本轮改为清晨冷蓝天光，去掉错误的武器；石桥、道路走向与旧木架不变";
        String prompt = ShortDramaLocationArtPrompt.reference(name, description, "chinese-3d", "前现代交通，资源有限", requirements);
        assertTrue(prompt.contains(name)); assertTrue(prompt.contains(description)); assertTrue(prompt.contains(requirements));
        assertTrue(prompt.contains("有参考图时只绑定其建筑几何")); assertTrue(prompt.contains("无参考图时按上述已冻结地点文案"));
        assertTrue(prompt.contains("优先于参考图和冻结文案中的旧时间、旧光色"));
        assertTrue(prompt.contains("不能为了保留参考而恢复已明确要求移除的物件"));
        assertTrue(prompt.contains("不得搬动建筑、改造道路")); assertTrue(prompt.contains("不自动替换已有批准图片"));
        assertTrue(prompt.contains("国风"));
    }
    @Test void optionalRequirementsSupportT2IAndExplicitLocationRevisionButNotAppearancePurpose() {
        assertFalse(ShortDramaLocationArtPrompt.isRevision(null, null));
        assertTrue(ShortDramaLocationArtPrompt.isRevision(null, "改为秋日午后中性天光"));
        assertTrue(ShortDramaLocationArtPrompt.isRevision("identity", "清空错误道具")); // historical shared DTO default
        assertTrue(ShortDramaLocationArtPrompt.isRevision("location_revision", "调整光色"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaLocationArtPrompt.isRevision("identity_revision", "调整光色"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaLocationArtPrompt.isRevision("location_revision", " "));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaLocationArtPrompt.isRevision(null, "光".repeat(4001)));
        assertTrue(ShortDramaLocationArtPrompt.isRevision(null, "光".repeat(4000)));
        assertEquals(ShortDramaAssetWorldPrompt.location("桥", "石桥", "chinese-3d", "当前世界"),
            ShortDramaLocationArtPrompt.reference("桥", "石桥", "chinese-3d", "当前世界", null));
    }
}
