package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.KpiSharedConfirm;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.KpiSharedConfirmMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.KpiSharedCollectionService;
import org.ruoyi.ipd.service.KpiSharedConfirmService;
import org.ruoyi.ipd.service.SystemConfigService;
import org.ruoyi.ipd.vo.KpiSharedConfirmView;
import org.ruoyi.ipd.vo.SharedKpiCollectView;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-1.2-BACKEND 共担 KPI 双组长确认单测（看板卡 56d97bb0；ZK 原型页 20「共担KPI双组长确认」）。
 *
 * <p>覆盖验收三点：
 * <ol>
 *   <li>空库 → 返回 []</li>
 *   <li>新建待确认条目 → 返回 KpiSharedConfirm[]</li>
 *   <li>双组长确认前置单人抛 40002 DUAL_SIGN_INCOMPLETE</li>
 * </ol>
 * 另覆盖：首签/次签/同人重签/非组长角色/状态机/CONFIRMED 再签/行不存在/Controller 包装/collect 联动等。
 *
 * <p>类内两层：内嵌 {@link ServiceLayer} 测真服务（mock mappers），平铺方法测 Controller（mock service）。
 * 单服务测试用真 KpiSharedConfirmService 拼装以走完状态机分支，避免把"业务规则"也 mock 掉。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KpiSharedConfirmControllerTest {

    @Mock IpdPermission permission;
    @Mock KpiSharedCollectionService collectService;
    @Mock KpiSharedConfirmService confirmService;
    @InjectMocks SharedKpiController controller;

    private static final Long PROJECT_ID = 201L;
    private static final String PERIOD = "2026-09";

    /* ====================== Controller 层 ====================== */

    @Test
    @DisplayName("[CT1] listConfirms 包装 + requireInternal 调用 + actor 透传")
    void ct1_listConfirms_wrapper() {
        IpdActor actor = actorGroupLeader(100L);
        KpiSharedConfirmView view = new KpiSharedConfirmView(
            "1", PERIOD, String.valueOf(PROJECT_ID), "项目X",
            "99", "归集组长", "K01", "销量/出货量达成率",
            new BigDecimal("0.15"), "2026-10-07T18:00",
            "PENDING", null, null, null, null, false);
        when(permission.requireInternal()).thenReturn(actor);
        when(confirmService.listConfirms(actor, PROJECT_ID, PERIOD, "PENDING"))
            .thenReturn(List.of(view));

        var resp = controller.listConfirms(PROJECT_ID, PERIOD, "PENDING");

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData()).hasSize(1);
        verify(confirmService, times(1)).listConfirms(actor, PROJECT_ID, PERIOD, "PENDING");
        verify(permission, times(1)).requireInternal();
    }

    @Test
    @DisplayName("[CT2] confirm 包装 + actor 透传")
    void ct2_confirm_wrapper() {
        IpdActor actor = actorGroupLeader(200L);
        KpiSharedConfirmService.ConfirmResult cr =
            new KpiSharedConfirmService.ConfirmResult(true, "CONFIRMED", "100", "200");
        when(permission.requireInternal()).thenReturn(actor);
        when(confirmService.confirm(actor, 1L)).thenReturn(cr);

        var resp = controller.confirm(1L);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        assertThat(resp.getData().confirmed()).isTrue();
        assertThat(resp.getData().status()).isEqualTo("CONFIRMED");
        verify(confirmService, times(1)).confirm(actor, 1L);
    }

    @Test
    @DisplayName("[CT3] collect 成功后联动 ensurePendingRows 生成 K01-K04 确认行")
    void ct3_collect_triggers_ensure() {
        IpdActor actor = actorGroupLeader(99L);
        when(permission.requireInternal()).thenReturn(actor);
        List<SharedKpiCollectView.Metric> metrics = List.of(
            metric("K01"), metric("K02"), metric("K03"), metric("K04"));
        SharedKpiCollectView view = new SharedKpiCollectView(
            PROJECT_ID, PERIOD, new BigDecimal("85.50"), metrics);
        when(collectService.collectSharedKpi(actor, isNull())).thenReturn(view);

        var resp = controller.collect(null);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        ArgumentCaptor<List<SharedKpiCollectView.Metric>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(confirmService, times(1))
            .ensurePendingRows(eq(PROJECT_ID), eq(PERIOD), eq(actor.id()), captor.capture());
        assertThat(captor.getValue()).extracting(SharedKpiCollectView.Metric::code)
            .containsExactly("K01", "K02", "K03", "K04");
    }

    @Test
    @DisplayName("[CT4] confirm 失败时不联动 ensure（collect 内事务内确保）")
    void ct4_collect_failure_no_ensure() {
        IpdActor actor = actorGroupLeader(99L);
        when(permission.requireInternal()).thenReturn(actor);
        when(collectService.collectSharedKpi(actor, isNull()))
            .thenThrow(new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "项目必须有且仅有一个在职MARKET_PM"));

        assertThatThrownBy(() -> controller.collect(null))
            .isInstanceOf(IpdBusinessException.class);

        verify(confirmService, never()).ensurePendingRows(any(), any(), any(), any());
    }

    /* ====================== Service 层：listConfirms ====================== */

    @Test
    @DisplayName("[S1] 空库 → 返回 []")
    void s1_listConfirms_empty() {
        ServiceLayer layer = new ServiceLayer();
        layer.projectMapper.selectById(PROJECT_ID).thenReturn(project());
        IpdActor actor = actorSuperAdmin(0L);

        List<KpiSharedConfirmView> views =
            layer.service.listConfirms(actor, PROJECT_ID, PERIOD, null);

        assertThat(views).isEmpty();
        verify(layer.confirmMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("[S2] 新建待确认条目返回 KpiSharedConfirm[]（含 K01/K02/K03/K04）")
    void s2_listConfirms_pending() {
        ServiceLayer layer = new ServiceLayer();
        Date future = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));
        List<KpiSharedConfirm> rows = List.of(
            pendingRow(1L, "K01", new BigDecimal("0.15"), future, null, null, null, null),
            pendingRow(2L, "K02", new BigDecimal("0.10"), future, null, null, null, null),
            pendingRow(3L, "K03", new BigDecimal("0.10"), future, null, null, null, null),
            pendingRow(4L, "K04", new BigDecimal("0.05"), future, null, null, null, null));
        layer.projectMapper.selectById(PROJECT_ID).thenReturn(project());
        layer.confirmMapper.selectList(any()).thenReturn(rows);
        layer.personMapper.selectById(99L).thenReturn(person(99L, "归集组长"));
        IpdActor actor = actorSuperAdmin(0L);

        List<KpiSharedConfirmView> views =
            layer.service.listConfirms(actor, PROJECT_ID, PERIOD, null);

        assertThat(views).hasSize(4);
        assertThat(views).extracting(KpiSharedConfirmView::metricCode)
            .containsExactly("K01", "K02", "K03", "K04");
        assertThat(views).allMatch(v -> "PENDING".equals(v.status()));
        assertThat(views).allMatch(KpiSharedConfirmView::confirmedByMe);
        assertThat(views.get(0).projectName()).isEqualTo("项目X");
        assertThat(views.get(0).personName()).isEqualTo("归集组长");
        assertThat(views.get(0).weight()).isEqualByComparingTo("0.15");
    }

    @Test
    @DisplayName("[S3] PENDING + deadlineAt 已过 → status 派生为 OVERDUE")
    void s3_listConfirms_overdueDerived() {
        ServiceLayer layer = new ServiceLayer();
        Date past = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        layer.projectMapper.selectById(PROJECT_ID).thenReturn(project());
        layer.confirmMapper.selectList(any()).thenReturn(
            List.of(pendingRow(1L, "K01", new BigDecimal("0.15"), past, null, null, null, null)));
        IpdActor actor = actorSuperAdmin(0L);

        List<KpiSharedConfirmView> views =
            layer.service.listConfirms(actor, PROJECT_ID, PERIOD, null);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo("OVERDUE");
    }

    @Test
    @DisplayName("[S4] status=OVERDUE 过滤 → 仅返回已逾期行（PENDING+过期）")
    void s4_listConfirms_filterOverdue() {
        ServiceLayer layer = new ServiceLayer();
        Date past = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        Date future = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5));
        layer.projectMapper.selectById(PROJECT_ID).thenReturn(project());
        layer.confirmMapper.selectList(any()).thenReturn(List.of(
            pendingRow(1L, "K01", new BigDecimal("0.15"), past, null, null, null, null),
            pendingRow(2L, "K02", new BigDecimal("0.10"), future, null, null, null, null)));
        IpdActor actor = actorSuperAdmin(0L);

        List<KpiSharedConfirmView> overdue = layer.service.listConfirms(
            actor, PROJECT_ID, PERIOD, "OVERDUE");

        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).metricCode()).isEqualTo("K01");
    }



    /* ====================== Service 层：ensurePendingRows ====================== */

    @Test
    @DisplayName("[E1] ensurePendingRows 新建 4 行（K01-K04）— insert × 4")
    void e1_ensureNewRows() {
        ServiceLayer layer = new ServiceLayer();
        when(layer.confirmMapper.selectOne(any())).thenReturn(null);
        List<SharedKpiCollectView.Metric> metrics = List.of(
            metric("K01"), metric("K02"), metric("K03"), metric("K04"));

        layer.service.ensurePendingRows(PROJECT_ID, PERIOD, 99L, metrics);

        ArgumentCaptor<KpiSharedConfirm> captor = ArgumentCaptor.forClass(KpiSharedConfirm.class);
        verify(layer.confirmMapper, times(4)).insert(captor.capture());
        List<KpiSharedConfirm> inserted = captor.getAllValues();
        assertThat(inserted).extracting(KpiSharedConfirm::getMetricCode)
            .containsExactly("K01", "K02", "K03", "K04");
        assertThat(inserted).allMatch(r -> "PENDING".equals(r.getStatus()));
        assertThat(inserted).allMatch(r -> r.getPersonId().equals(99L));
        assertThat(inserted).allMatch(r -> r.getDeadlineAt() != null);
        assertThat(inserted).extracting(KpiSharedConfirm::getWeight)
            .containsExactly(new BigDecimal("0.15"), new BigDecimal("0.10"),
                new BigDecimal("0.10"), new BigDecimal("0.05"));
    }

    @Test
    @DisplayName("[E2] ensurePendingRows 遇 CONFIRMED 已确认行 → 重置为 PENDING（双签置空）")
    void e2_ensureResetConfirmedRow() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm confirmed = pendingRow(1L, "K01", new BigDecimal("0.15"),
            new Date(), 100L, new Date(), 200L, new Date());
        confirmed.setStatus("CONFIRMED");
        when(layer.confirmMapper.selectOne(any())).thenReturn(confirmed);

        layer.service.ensurePendingRows(PROJECT_ID, PERIOD, 99L,
            List.of(metric("K01")));

        verify(layer.confirmMapper, times(1)).update(isNull(), any());
        verify(layer.confirmMapper, never()).insert(any());
    }

    @Test
    @DisplayName("[E3] ensurePendingRows 遇 PENDING 已存在行 → 仅更新 deadlineAt")
    void e3_ensureRefreshDeadline() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm pending = pendingRow(1L, "K01", new BigDecimal("0.15"),
            new Date(0), null, null, null, null);
        when(layer.confirmMapper.selectOne(any())).thenReturn(pending);

        layer.service.ensurePendingRows(PROJECT_ID, PERIOD, 99L,
            List.of(metric("K01")));

        verify(layer.confirmMapper, times(1)).updateById(any());
        verify(layer.confirmMapper, never()).insert(any());
    }

    @Test
    @DisplayName("[E4] ensurePendingRows period 非法 / metrics 空 → 静默返回（归集主流程已校验）")
    void e4_ensureNullSafe() {
        ServiceLayer layer = new ServiceLayer();
        layer.service.ensurePendingRows(PROJECT_ID, null, 99L, List.of(metric("K01")));
        layer.service.ensurePendingRows(PROJECT_ID, "2026-99", 99L, List.of(metric("K01")));
        layer.service.ensurePendingRows(PROJECT_ID, PERIOD, 99L, List.of());

        verify(layer.confirmMapper, never()).selectOne(any());
        verify(layer.confirmMapper, never()).insert(any());
    }

    /* ====================== Service 层：confirm 双签 ====================== */

    @Test
    @DisplayName("[C1] 首签 → firstConfirmedBy/At 填充、status 仍 PENDING、confirmed=false")
    void c1_confirm_firstSign() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm row = pendingRow(1L, "K01", new BigDecimal("0.15"),
            futureDeadline(), null, null, null, null);
        when(layer.confirmMapper.selectById(1L)).thenReturn(row);
        IpdActor actor = actorGroupLeader(100L);

        KpiSharedConfirmService.ConfirmResult cr = layer.service.confirm(actor, 1L);

        assertThat(cr.confirmed()).isFalse();
        assertThat(cr.status()).isEqualTo("PENDING");
        assertThat(cr.firstConfirmedBy()).isEqualTo("100");
        assertThat(cr.secondConfirmedBy()).isNull();
        assertThat(row.getFirstConfirmedBy()).isEqualTo(100L);
        assertThat(row.getFirstConfirmedAt()).isNotNull();
        assertThat(row.getStatus()).isEqualTo("PENDING");
        verify(layer.confirmMapper, times(1)).updateById(row);
        verify(layer.auditLogService, times(1)).append(any());
    }

    @Test
    @DisplayName("[C2] 第二位不同组长签 → second 填充、status=CONFIRMED、confirmed=true")
    void c2_confirm_secondSign() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm row = pendingRow(1L, "K01", new BigDecimal("0.15"),
            futureDeadline(), 100L, new Date(), null, null);
        when(layer.confirmMapper.selectById(1L)).thenReturn(row);
        IpdActor actor = actorGroupLeader(200L);

        KpiSharedConfirmService.ConfirmResult cr = layer.service.confirm(actor, 1L);

        assertThat(cr.confirmed()).isTrue();
        assertThat(cr.status()).isEqualTo("CONFIRMED");
        assertThat(cr.firstConfirmedBy()).isEqualTo("100");
        assertThat(cr.secondConfirmedBy()).isEqualTo("200");
        assertThat(row.getSecondConfirmedBy()).isEqualTo(200L);
        assertThat(row.getSecondConfirmedAt()).isNotNull();
        assertThat(row.getStatus()).isEqualTo("CONFIRMED");
        verify(layer.confirmMapper, times(1)).updateById(row);
    }

    @Test
    @DisplayName("[C3] 双签前置单人（同人重签）→ 抛 40002 DUAL_SIGN_INCOMPLETE")
    void c3_confirm_sameLeaderThrows40002() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm row = pendingRow(1L, "K01", new BigDecimal("0.15"),
            futureDeadline(), 100L, new Date(), null, null);
        when(layer.confirmMapper.selectById(1L)).thenReturn(row);
        IpdActor actor = actorGroupLeader(100L);

        assertThatThrownBy(() -> layer.service.confirm(actor, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.DUAL_SIGN_INCOMPLETE);

        verify(layer.confirmMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("[C4] 非组长角色（MARKET_PM）confirm → FORBIDDEN，不查 DB")
    void c4_confirm_notLeader() {
        ServiceLayer layer = new ServiceLayer();
        IpdActor actor = new IpdActor(300L, "市场PM", "MARKET_PM", 10L);

        assertThatThrownBy(() -> layer.service.confirm(actor, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        verify(layer.confirmMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("[C5] 已 CONFIRMED 再签 → STATE_CONFLICT")
    void c5_confirm_alreadyConfirmed() {
        ServiceLayer layer = new ServiceLayer();
        KpiSharedConfirm row = pendingRow(1L, "K01", new BigDecimal("0.15"),
            futureDeadline(), 100L, new Date(), 200L, new Date());
        row.setStatus("CONFIRMED");
        when(layer.confirmMapper.selectById(1L)).thenReturn(row);
        IpdActor actor = actorGroupLeader(300L);

        assertThatThrownBy(() -> layer.service.confirm(actor, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[C6] 行不存在 → NOT_FOUND")
    void c6_confirm_notFound() {
        ServiceLayer layer = new ServiceLayer();
        when(layer.confirmMapper.selectById(99L)).thenReturn(null);
        IpdActor actor = actorGroupLeader(100L);

        assertThatThrownBy(() -> layer.service.confirm(actor, 99L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("[C7] confirmId=null → PARAM_INVALID")
    void c7_confirm_nullId() {
        ServiceLayer layer = new ServiceLayer();
        IpdActor actor = actorGroupLeader(100L);

        assertThatThrownBy(() -> layer.service.confirm(actor, null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    /* ====================== helpers ====================== */

    /** 内嵌"真 service + mock mappers"测试装配器（避免污染平铺字段） */
    private static final class ServiceLayer {
        final KpiSharedConfirmMapper confirmMapper = org.mockito.Mockito.mock(KpiSharedConfirmMapper.class);
        final ProjectMapper projectMapper = org.mockito.Mockito.mock(ProjectMapper.class);
        final ProjectMemberMapper memberMapper = org.mockito.Mockito.mock(ProjectMemberMapper.class);
        final PersonMapper personMapper = org.mockito.Mockito.mock(PersonMapper.class);
        final AuditLogService auditLogService = org.mockito.Mockito.mock(AuditLogService.class);
        final SystemConfigService systemConfigService = null; // 默认 5 工作日
        final KpiSharedConfirmService service = new KpiSharedConfirmService(
            confirmMapper, projectMapper, memberMapper, personMapper, auditLogService, systemConfigService);
    }

    private static IpdActor actorGroupLeader(long id) {
        return new IpdActor(id, "组长" + id, "GROUP_LEADER", 10L);
    }
    private static IpdActor actorSuperAdmin(long id) {
        return new IpdActor(id, "超管", "SUPER_ADMIN", null);
    }
    private static Project project() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setName("项目X");
        return p;
    }
    private static Person person(long id, String name) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        return p;
    }
    private static SharedKpiCollectView.Metric metric(String code) {
        return new SharedKpiCollectView.Metric(
            code, "MANUAL", "100", "120",
            new BigDecimal("80"),
            new BigDecimal(code.equals("K01") ? "0.15" : code.equals("K03") ? "0.10" : code.equals("K02") ? "0.10" : "0.05"),
            true, code);
    }
    private static Date futureDeadline() {
        return new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));
    }
    private static KpiSharedConfirm pendingRow(long id, String code, BigDecimal weight, Date deadline,
                                               Long firstBy, Date firstAt, Long secondBy, Date secondAt) {
        return KpiSharedConfirm.builder()
            .id(id).projectId(PROJECT_ID).period(PERIOD)
            .personId(99L).metricCode(code)
            .weight(weight).deadlineAt(deadline).status("PENDING")
            .firstConfirmedBy(firstBy).firstConfirmedAt(firstAt)
            .secondConfirmedBy(secondBy).secondConfirmedAt(secondAt)
            .build();
    }
}


