package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.service.AllowanceLedgerService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W4-D AllowanceLedgerController 单测（3 维度）+ W5-E-2.3 IDOR 修复升级
 *
 * <p>覆盖 3 维度（件 3 必查）：
 * <ol>
 *   <li>正常路径：listLedger 按 period + 可选 personId 过滤 / pendingStop 同月过滤 / autoScan 返回计数</li>
 *   <li>边界：list/pendingStop 返回空列表时不抛 / autoScan 返回 0</li>
 *   <li>权限：autoScan 仅 SUPER_ADMIN，非超管调用抛 IpdPermissionException（list/pendingStop 由 requireInternal 兜底）</li>
 * </ol>
 *
 * <p>W5-E-2.3（P0 #3）：service 三方法签名加 {@code IpdActor} 第一参数后，本测试 6 个用例全部升级为
 * 3-arg 调用；新增 3 个用例断言 Controller 把 {@code requireInternal()/requireAdmin()} 捕获的 actor
 * 原样传给 service（actor 传递链完整性——service 层 UNAUTHORIZED/FORBIDDEN 守卫的入参来源）。
 * actor null → UNAUTHORIZED、autoScan 非 SUPER_ADMIN → FORBIDDEN 的 service 层行为由
 * {@code P331AcceptanceTest} 以真实 service 实例覆盖。
 *
 * <p>AC：AC-INC-03/04/05/07/08；BR：BR-INC-02/03/11。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AllowanceLedgerControllerTest {

    @Mock
    private IpdPermission ipdPermission;

    @Mock
    private AllowanceLedgerService service;

    @InjectMocks
    private AllowanceLedgerController controller;

    /* ====================== 1. listLedger 正常路径 ====================== */

    @Test
    @DisplayName("[W4-D-1] listLedger 正常返回 + 不带 personId 过滤")
    void listLedger_noPersonId() {
        IpdActor actor = new IpdActor(1L, "u", "MARKET_PM", 1L);
        List<AllowanceLedger> rows = List.of(buildLedger(101L, 201L, "3000.00"));
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.list(actor, "2026-09", null)).thenReturn(rows);

        ApiV1Response<List<AllowanceLedger>> resp = controller.listLedger("2026-09", null);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(1).first()
            .extracting(AllowanceLedger::getPersonId, AllowanceLedger::getProjectId)
            .containsExactly(101L, 201L);
        verify(service, times(1)).list(actor, "2026-09", null);
    }

    @Test
    @DisplayName("[W4-D-2] listLedger 过滤 personId：personId=101 只返回该人员记录")
    void listLedger_filterByPersonId() {
        IpdActor actor = new IpdActor(2L, "u", "RD_PM", 1L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.list(actor, "2026-09", 101L)).thenReturn(List.of(buildLedger(101L, 201L, "3000.00")));

        ApiV1Response<List<AllowanceLedger>> resp = controller.listLedger("2026-09", 101L);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(1);
        verify(service, times(1)).list(eq(actor), eq("2026-09"), eq(101L));
    }

    /* ====================== 2. pendingStop 正常路径 ====================== */

    @Test
    @DisplayName("[W4-D-3] listPendingStop 正常返回：传 period 返回服务列表")
    void listPendingStop_returnsRows() {
        IpdActor actor = new IpdActor(3L, "u", "GROUP_LEADER", 1L);
        AllowanceLedger l = buildLedger(101L, 201L, "0.00");
        l.setStopReason("SCORE_BELOW_60");
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.pendingStop(actor, "2026-09")).thenReturn(List.of(l));

        ApiV1Response<List<AllowanceLedger>> resp = controller.listPendingStop("2026-09");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getStopReason()).isEqualTo("SCORE_BELOW_60");
        verify(service, times(1)).pendingStop(actor, "2026-09");
    }

    @Test
    @DisplayName("[W4-D-4] listPendingStop 空月份返回空列表")
    void listPendingStop_empty() {
        IpdActor actor = new IpdActor(4L, "u", "MARKET_PM", 1L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.pendingStop(actor, "2026-09")).thenReturn(Collections.emptyList());

        ApiV1Response<List<AllowanceLedger>> resp = controller.listPendingStop("2026-09");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).isEmpty();
    }

    /* ====================== 3. autoScan 权限 + 正常路径 ====================== */

    @Test
    @DisplayName("[W4-D-5] autoScan 超管路径：返回受影响记录数 3")
    void autoScan_adminReturnsCount() {
        IpdActor admin = new IpdActor(999L, "超管", "SUPER_ADMIN", null);
        when(ipdPermission.requireAdmin()).thenReturn(admin);
        when(service.autoScan(admin, "2026-09")).thenReturn(3);

        ApiV1Response<Integer> resp = controller.autoScan("2026-09");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).isEqualTo(3);
        verify(service, times(1)).autoScan(admin, "2026-09");
    }

    @Test
    @DisplayName("[W4-D-6] autoScan 非超管抛 IpdPermissionException：不调 service")
    void autoScan_nonAdminRejected() {
        // 模拟 requireAdmin 抛 IpdPermissionException（403 FORBIDDEN）
        when(ipdPermission.requireAdmin()).thenThrow(
            new IpdPermissionException(403, org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> controller.autoScan("2026-09"))
            .isInstanceOf(IpdPermissionException.class);

        // 权限失败时 service.autoScan 不应被调用（含 actor 的 3-arg 签名）
        verify(service, never()).autoScan(any(IpdActor.class), anyString());
    }

    /* ====================== 4. W5-E-2.3 actor 传递链（新增） ====================== */

    @Test
    @DisplayName("[W5-E-2.3-7] listLedger 把 requireInternal 捕获的 actor 原样传给 service → 200")
    void listLedger_passesRequireInternalActorToService() {
        IpdActor actor = new IpdActor(7L, "张三", "MARKET_PM", 1L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.list(any(IpdActor.class), eq("2026-09"), isNull()))
            .thenReturn(List.of(buildLedger(101L, 201L, "3000.00")));

        ApiV1Response<List<AllowanceLedger>> resp = controller.listLedger("2026-09", null);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        ArgumentCaptor<IpdActor> captor = ArgumentCaptor.forClass(IpdActor.class);
        verify(service).list(captor.capture(), eq("2026-09"), isNull());
        // actor 传递链完整：service 收到的就是会话 actor（record 结构等值）
        assertThat(captor.getValue()).isEqualTo(actor);
    }

    @Test
    @DisplayName("[W5-E-2.3-8] listPendingStop 把 requireInternal 捕获的 actor 原样传给 service → 200")
    void listPendingStop_passesRequireInternalActorToService() {
        IpdActor actor = new IpdActor(8L, "李四", "RD_PM", 2L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(service.pendingStop(any(IpdActor.class), eq("2026-09"))).thenReturn(Collections.emptyList());

        ApiV1Response<List<AllowanceLedger>> resp = controller.listPendingStop("2026-09");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        verify(service).pendingStop(actor, "2026-09");
    }

    @Test
    @DisplayName("[W5-E-2.3-9] autoScan 把 requireAdmin 捕获的超管 actor 原样传给 service（非超管 FORBIDDEN 由 W4-D-6 + service 层双兜底）")
    void autoScan_passesRequireAdminActorToService() {
        IpdActor admin = new IpdActor(999L, "超管", "SUPER_ADMIN", null);
        when(ipdPermission.requireAdmin()).thenReturn(admin);
        when(service.autoScan(any(IpdActor.class), eq("2026-09"))).thenReturn(5);

        ApiV1Response<Integer> resp = controller.autoScan("2026-09");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).isEqualTo(5);
        ArgumentCaptor<IpdActor> captor = ArgumentCaptor.forClass(IpdActor.class);
        verify(service).autoScan(captor.capture(), eq("2026-09"));
        assertThat(captor.getValue()).isEqualTo(admin);
    }

    /* ====================== 测试工具 ====================== */

    /**
     * 构造一条 AllowanceLedger 草稿（finalAmount / capApplied 已填）。
     */
    private AllowanceLedger buildLedger(Long personId, Long projectId, String finalAmount) {
        return AllowanceLedger.builder()
            .personId(personId)
            .projectId(projectId)
            .month("2026-09")
            .lockedLevel("L3")
            .baseAmount(new BigDecimal("3000.00"))
            .finalAmount(new BigDecimal(finalAmount))
            .capApplied("0")
            .build();
    }
}
