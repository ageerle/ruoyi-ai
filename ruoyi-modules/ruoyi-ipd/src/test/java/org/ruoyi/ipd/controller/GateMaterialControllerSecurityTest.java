/**
 * GateMaterialController 安全守卫测试（[SEC-FIX] 2026-09-06）。
 * - projectId 与 gateId 不匹配 → 抛 50001 RESOURCE_NOT_FOUND
 * - gateId 不存在 → 同样抛
 * - 关联通过 → 调 service
 */
package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.service.GateMaterialChecker;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class GateMaterialControllerSecurityTest {

    @Mock private GateMaterialChecker checker;
    @Mock private GateMapper gateMapper;
    @InjectMocks private GateMaterialController controller;

    @Test
    @DisplayName("[SEC-FIX] gateId 不存在 → 抛 RESOURCE_NOT_FOUND，不调 service")
    void gateNotFound() {
        when(gateMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> controller.materials(1L, 100L))
            .isInstanceOf(IpdBusinessException.class);
        verify(checker, never()).listMaterialStatus(any(), any());
    }

    @Test
    @DisplayName("[SEC-FIX] projectId 与 Gate.projectId 不匹配 → 抛 RESOURCE_NOT_FOUND")
    void projectIdMismatch() {
        Gate gate = new Gate();
        gate.setId(1L);
        gate.setProjectId(999L);
        when(gateMapper.selectById(1L)).thenReturn(gate);
        assertThatThrownBy(() -> controller.materials(1L, 100L))
            .isInstanceOf(IpdBusinessException.class);
        verify(checker, never()).listMaterialStatus(any(), any());
    }

    @Test
    @DisplayName("[SEC-FIX] gateId 与 projectId 匹配 → 正常调 service")
    void happyPath() {
        Gate gate = new Gate();
        gate.setId(1L);
        gate.setProjectId(100L);
        when(gateMapper.selectById(1L)).thenReturn(gate);
        when(checker.listMaterialStatus(1L, 100L)).thenReturn(Map.of("isReady", true));
        assertThat(controller.materials(1L, 100L).getData().get("isReady")).isEqualTo(true);
        verify(checker, times(1)).listMaterialStatus(1L, 100L);
    }
}
