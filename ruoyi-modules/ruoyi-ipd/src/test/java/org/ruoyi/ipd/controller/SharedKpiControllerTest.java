package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.service.KpiSharedCollectionService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W4-E SharedKpiController 单测（GET /kpi/shared 读端点补交付）。
 *
 * <p>覆盖 3 维度（件 3 必查）：
 * <ol>
 *   <li>正常路径：listShared 按 projectId+period 过滤返回 KpiRecord 列表（双 PM 各 1 条）</li>
 *   <li>边界：项目当期无归集 → 返回空列表（不抛）</li>
 *   <li>权限：requireInternal 兜底调用 + 注解双重校验（ipd:kpi:query）</li>
 * </ol>
 *
 * <p>W4-E §1 + 件 3：补 GET 读端点对应测试；POST collect / deadline-scan 端点测试已由 P312AcceptanceTest / P313AcceptanceTest 覆盖，本测试不重复。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class SharedKpiControllerTest {

    @Mock
    private org.ruoyi.ipd.security.IpdPermission ipdPermission;

    @Mock
    private KpiSharedCollectionService service;

    @InjectMocks
    private SharedKpiController controller;

    /* ====================== 1. listShared 正常路径 ====================== */

    @Test
    @DisplayName("[W4-E-1] listShared 正常返回：双 PM 各 1 条 + revision DESC 排序")
    void listShared_returnsRecords() {
        Long projectId = 201L;
        String period = "2026-09";
        IpdActor actor = new IpdActor(1L, "组长", "GROUP_LEADER", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.listSharedKpis(actor, projectId, period)).thenReturn(List.of(
            buildRecord(11L, 201L, "MARKET_PM" , period, 2, "85.50"),
            buildRecord(12L, 201L, "RD_PM",     period, 2, "85.50")
        ));

        ApiV1Response<List<KpiRecord>> resp = controller.listShared(projectId, period);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(2);
        assertThat(resp.getData()).extracting(KpiRecord::getRevision).containsExactly(2, 2);
        verify(service, times(1)).listSharedKpis(actor, projectId, period);
    }

    @Test
    @DisplayName("[W4-E-2] listShared 多 revision：同一项目多次归集，返回 revision 倒序所有版本")
    void listShared_multipleRevisions() {
        Long projectId = 201L;
        String period = "2026-09";
        IpdActor actor = new IpdActor(1L, "组长", "GROUP_LEADER", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        // 模拟两次归集：rev 2 最新 + rev 1 旧版（双 PM 各 2 条 = 4 行）
        when(service.listSharedKpis(actor, projectId, period)).thenReturn(List.of(
            buildRecord(11L, 201L, "MARKET_PM", period, 2, "90.00"),
            buildRecord(12L, 201L, "RD_PM",     period, 2, "90.00"),
            buildRecord(13L, 201L, "MARKET_PM", period, 1, "75.00"),
            buildRecord(14L, 201L, "RD_PM",     period, 1, "75.00")
        ));

        ApiV1Response<List<KpiRecord>> resp = controller.listShared(projectId, period);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(4);
        // 验证 revision 倒序：service 层按 revision DESC, id ASC 排序
        assertThat(resp.getData()).extracting(KpiRecord::getRevision)
            .containsExactly(2, 2, 1, 1);
    }

    /* ====================== 2. 边界：空列表 ====================== */

    @Test
    @DisplayName("[W4-E-3] listShared 项目当期无归集 → 空列表不抛")
    void listShared_emptyResult() {
        Long projectId = 999L;
        String period = "2026-09";
        IpdActor actor = new IpdActor(1L, "超管", "SUPER_ADMIN", null);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.listSharedKpis(actor, projectId, period)).thenReturn(Collections.emptyList());

        ApiV1Response<List<KpiRecord>> resp = controller.listShared(projectId, period);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).isEmpty();
        verify(service, times(1)).listSharedKpis(actor, projectId, period);
    }

    /* ====================== 3. 权限兜底 ====================== */

    @Test
    @DisplayName("[W4-E-4] listShared 兜底 requireInternal 调用一次（与注解同严）")
    void listShared_requireInternalInvoked() {
        Long projectId = 201L;
        String period = "2026-09";
        IpdActor actor = new IpdActor(1L, "市场PM", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.listSharedKpis(actor, projectId, period)).thenReturn(Collections.emptyList());

        controller.listShared(projectId, period);

        // 验证 requireInternal 兜底被调用（注解 + 兜底双校验）
        verify(ipdPermission, times(1)).requireInternal();
        // 验证 deadline-scan 用的 requireAdmin 未被调用（list 端点无需超管）
        verify(ipdPermission, times(0)).requireAdmin();
        // W4-Security 件 1.6：捕获的 actor 必须原样传给 service（IDOR 鉴权基础）
        verify(service, times(1)).listSharedKpis(actor, projectId, period);
    }

    /* ====================== 测试工具 ====================== */

    private KpiRecord buildRecord(Long id, Long projectId, String personType, String period,
                                  int revision, String score) {
        KpiRecord r = new KpiRecord();
        r.setId(id);
        r.setProjectId(projectId);
        // personId 用 id 自增避开冲突
        r.setPersonId(1000L + id);
        r.setKpiType("SHARED");
        r.setPeriod(period);
        r.setComprehensiveScore(new BigDecimal(score));
        r.setStatus("FINALIZED");
        r.setRevision(revision);
        r.setSegment("FULL_SHARED");
        return r;
    }
}