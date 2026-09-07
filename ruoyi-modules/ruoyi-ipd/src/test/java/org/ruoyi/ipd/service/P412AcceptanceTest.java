package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.dto.GuestDemandUpdateReq;
import org.ruoyi.ipd.dto.GuestDemandView;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P4-1.2 查询码隔离与受理前撤回补充 验收测试 (BR-REQ-03/03a/03b; AC-REQ-04 + AC-REQ-04b)
 *
 * <p>卡面统一收口 3 项验收 AC：
 * <ol>
 *   <li>AC-REQ-03 BR-REQ-03：8 位查询码 ^[A-Z0-9]{8}$ 全局唯一（uk_req_query_code 唯一索引，冲突 5 次重试）</li>
 *   <li>AC-REQ-04：受理前（status=SUBMITTED）可补充 functionalRequirement / contact，受理后锁定</li>
 *   <li>AC-REQ-04b：受理前（status=SUBMITTED）可撤回（→ WITHDRAWN 终态），受理后撤回拒绝</li>
 * </ol>
 *
 * <p>实现见 {@link GuestDemandService}（P4-1.2）；本卡只验收、不可改业务代码。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class P412AcceptanceTest {

    @Mock private RequirementMapper requirementMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AuditLogService auditLogService;

    private GuestDemandService service;

    private static final String QUERY_CODE = "AB12CD34";

    @BeforeEach
    void setUp() {
        service = new GuestDemandService(requirementMapper, productMapper, projectMemberMapper, auditLogService);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Requirement reqWithStatus(String status) {
        Requirement r = Requirement.builder()
            .id(1L).queryCode(QUERY_CODE).status(status)
            .contact("13800000000")
            .content("原始需求内容：希望增加离线导出功能")
            .build();
        return r;
    }

    /** 通过 doAnswer 拦截 updateById 调用，避开 BaseMapper 双签名歧义。 */
    private AtomicReference<Requirement> captureUpdate() {
        AtomicReference<Requirement> ref = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof Requirement) {
                ref.set((Requirement) arg);
            }
            return 1;
        }).when(requirementMapper).updateById(org.mockito.ArgumentMatchers.any(Requirement.class));
        return ref;
    }

    /* ============================================================
     *  AC-REQ-03  查询码 8 位 + 全局唯一
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-03] 查询码格式 ^[A-Z0-9]{8}$ 锁定，长度 = 8")
    void AC_REQ_03_查询码格式锁定() {
        String code = GuestDemandService.randomCode();
        assertThat(code).hasSize(8);
        assertThat(GuestDemandService.QUERY_CODE_PATTERN.matcher(code).matches()).isTrue();
    }

    @Test
    @DisplayName("[AC-REQ-03] 查询码确定性：randomCode 全大写字母数字，无小写无特殊字符")
    void AC_REQ_03_查询码确定性() {
        for (int i = 0; i < 100; i++) {
            String code = GuestDemandService.randomCode();
            assertThat(code).matches("^[A-Z0-9]{8}$");
        }
    }

    @Test
    @DisplayName("[AC-REQ-03] 查询码空间：36^8 ≈ 2.8 万亿 ⇒ 短时碰撞概率忽略")
    void AC_REQ_03_查询码空间() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(GuestDemandService.randomCode());
        }
        assertThat(codes.size()).isGreaterThanOrEqualTo(999);
    }

    @Test
    @DisplayName("[AC-REQ-03] generateUniqueCode 冲突重试：DB 返回 count=1,1,0 第三次成功")
    void AC_REQ_03_碰撞重试成功() throws Exception {
        when(requirementMapper.selectCount(any())).thenReturn(1L, 1L, 0L);
        java.lang.reflect.Method m = GuestDemandService.class.getDeclaredMethod("generateUniqueCode");
        m.setAccessible(true);
        String code = (String) m.invoke(service);
        assertThat(code).matches("^[A-Z0-9]{8}$");
        org.mockito.Mockito.verify(requirementMapper, org.mockito.Mockito.times(3)).selectCount(any());
    }

    @Test
    @DisplayName("[AC-REQ-03] generateUniqueCode 5 次仍冲突 ⇒ INTERNAL_ERROR（极端降级）")
    void AC_REQ_03_5次碰撞降级() throws Exception {
        when(requirementMapper.selectCount(any())).thenReturn(1L);
        java.lang.reflect.Method m = GuestDemandService.class.getDeclaredMethod("generateUniqueCode");
        m.setAccessible(true);
        assertThatThrownBy(() -> m.invoke(service))
            .hasRootCauseInstanceOf(IpdBusinessException.class);
    }

    /* ============================================================
     *  AC-REQ-04  受理前补充（functionalRequirement / contact）
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-04] 受理前补充 content+contact ⇒ 更新字段 + audit supplement")
    void AC_REQ_04_受理前补充成功() {
        Requirement r = reqWithStatus("SUBMITTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);
        AtomicReference<Requirement> updated = captureUpdate();

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_SUPPLEMENT,
            "希望增加离线导出功能并支持按月归档",
            "13900000000");
        GuestDemandView view = service.supplement(QUERY_CODE, patch, "1.2.3.4", "Mozilla/5.0");

        assertThat(view.queryCode()).isEqualTo(QUERY_CODE);
        assertThat(view.status()).isEqualTo("SUBMITTED");
        assertThat(updated.get()).isNotNull();
        assertThat(updated.get().getContent()).isEqualTo("希望增加离线导出功能并支持按月归档");
        assertThat(updated.get().getContact()).isEqualTo("13900000000");

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("supplement");
    }

    @Test
    @DisplayName("[AC-REQ-04] 受理后补充 content ⇒ STATE_CONFLICT（不可改原文）")
    void AC_REQ_04_受理后补充拒绝() {
        Requirement r = reqWithStatus("ACCEPTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);
        AtomicReference<Requirement> updated = captureUpdate();
        org.mockito.Mockito.verify(requirementMapper, org.mockito.Mockito.never()).updateById(org.mockito.ArgumentMatchers.any(Requirement.class));

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_SUPPLEMENT,
            "改不动的原文", null);
        assertThatThrownBy(() -> service.supplement(QUERY_CODE, patch, "ip", "ua"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        assertThat(updated.get()).isNull();
    }

    @Test
    @DisplayName("[AC-REQ-04] 受理后补充 contact ⇒ STATE_CONFLICT（不可改联系信息）")
    void AC_REQ_04_受理后补充联系拒绝() {
        Requirement r = reqWithStatus("ROUTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_SUPPLEMENT,
            null, "13811111111");
        assertThatThrownBy(() -> service.supplement(QUERY_CODE, patch, "ip", "ua"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[AC-REQ-04] action≠SUPPLEMENT ⇒ PARAM_INVALID（与 WITHDRAW 混用防 state-drift）")
    void AC_REQ_04_非SUPPLEMENT拒绝() {
        Requirement r = reqWithStatus("SUBMITTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_WITHDRAW, null, null);
        assertThatThrownBy(() -> service.supplement(QUERY_CODE, patch, "ip", "ua"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    /* ============================================================
     *  AC-REQ-04b  受理前撤回（SUBMITTED → WITHDRAWN）
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-04b] 受理前撤回 SUBMITTED → WITHDRAWN（终态）")
    void AC_REQ_04b_受理前撤回成功() {
        Requirement r = reqWithStatus("SUBMITTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);
        AtomicReference<Requirement> updated = captureUpdate();

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_WITHDRAW, null, null);
        GuestDemandView view = service.withdraw(QUERY_CODE, patch, "1.2.3.4", "Mozilla/5.0");

        assertThat(view.status()).isEqualTo("WITHDRAWN");
        assertThat(updated.get()).isNotNull();
        assertThat(updated.get().getStatus()).isEqualTo("WITHDRAWN");

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("withdraw");
    }

    @Test
    @DisplayName("[AC-REQ-04b] 受理后撤回 ⇒ STATE_CONFLICT（ACCEPTED/ROUTED/WITHDRAWN/CLOSED 均拒绝）")
    void AC_REQ_04b_受理后撤回拒绝() {
        for (String terminal : new String[] {"ACCEPTED", "ROUTED", "WITHDRAWN", "CLOSED"}) {
            Requirement r = reqWithStatus(terminal);
            when(requirementMapper.selectOne(any())).thenReturn(r);

            GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
                GuestDemandUpdateReq.ACTION_WITHDRAW, null, null);
            assertThatThrownBy(() -> service.withdraw(QUERY_CODE, patch, "ip", "ua"))
                .as("status=%s 应拒绝撤回", terminal)
                .isInstanceOf(IpdBusinessException.class)
                .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        }
    }

    @Test
    @DisplayName("[AC-REQ-04b] WITHDRAWN 终态再次撤回仍拒绝（幂等拒绝 ⇒ 防重复撤回攻击）")
    void AC_REQ_04b_撤回终态重复拒绝() {
        Requirement r = reqWithStatus("WITHDRAWN");
        when(requirementMapper.selectOne(any())).thenReturn(r);

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_WITHDRAW, null, null);
        assertThatThrownBy(() -> service.withdraw(QUERY_CODE, patch, "ip", "ua"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[AC-REQ-04b] action≠WITHDRAW ⇒ PARAM_INVALID（与 SUPPLEMENT 混用防误操作）")
    void AC_REQ_04b_非WITHDRAW拒绝() {
        Requirement r = reqWithStatus("SUBMITTED");
        when(requirementMapper.selectOne(any())).thenReturn(r);

        GuestDemandUpdateReq patch = new GuestDemandUpdateReq(
            GuestDemandUpdateReq.ACTION_SUPPLEMENT, null, null);
        assertThatThrownBy(() -> service.withdraw(QUERY_CODE, patch, "ip", "ua"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("[AC-REQ-03] validateQueryCode 守卫：格式非法 / 长度不对 ⇒ NOT_FOUND")
    void AC_REQ_03_validateQueryCode守卫() {
        for (String bad : new String[] {null, "", "abc", "abc12345", "AB12CD3", "AB12CD34X", "ab12cd34", "AB-12345"}) {
            assertThatThrownBy(() -> service.supplement(bad,
                new GuestDemandUpdateReq(GuestDemandUpdateReq.ACTION_SUPPLEMENT, "足够长的需求内容描述", null),
                "ip", "ua"))
                .as("非法 queryCode='%s' 应拒绝", bad)
                .isInstanceOf(IpdBusinessException.class)
                .extracting("errorCode").isEqualTo(ApiV1ErrorCode.NOT_FOUND);
            assertThatThrownBy(() -> service.withdraw(bad,
                new GuestDemandUpdateReq(GuestDemandUpdateReq.ACTION_WITHDRAW, null, null),
                "ip", "ua"))
                .as("非法 queryCode='%s' 应拒绝", bad)
                .isInstanceOf(IpdBusinessException.class)
                .extracting("errorCode").isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        }
    }
}