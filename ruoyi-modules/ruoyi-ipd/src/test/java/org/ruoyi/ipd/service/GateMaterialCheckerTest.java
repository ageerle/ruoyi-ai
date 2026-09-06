/**
 * Gate 材料齐套性测试（[CONSISTENCY-15]）——验证 listMaterialStatus 返回结构。
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GateMaterialCheckerTest {

    @org.mockito.Mock
    private StageActionMapper stageActionMapper;
    @org.mockito.Mock
    private DeliverableMapper deliverableMapper;

    @org.mockito.InjectMocks
    private GateMaterialChecker checker;

    @org.junit.jupiter.api.Test
    @DisplayName("[CONSISTENCY-15] 空项目 → total=0, isReady=false")
    void emptyProject() {
        org.mockito.Mockito.when(stageActionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(deliverableMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());
        java.util.Map<String, Object> out = checker.listMaterialStatus(1L, 100L);
        org.assertj.core.api.Assertions.assertThat(((Number) out.get("total")).longValue()).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat((Boolean) out.get("isReady")).isFalse();
    }
}
