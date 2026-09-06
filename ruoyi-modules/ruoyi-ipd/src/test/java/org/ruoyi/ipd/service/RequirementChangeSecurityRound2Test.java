package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-REV round 2：RequirementChangeService 安全修复回归（4 项）：
 * <ul>
 *   <li>Bug#10 高危：state-drift —— applyApprovedToRequirement 失败必须触发 rollback</li>
 *   <li>Bug#11 中危：sensitive-observability —— 审计失败原因仅落异常类名</li>
 *   <li>Bug#12 中危：missing-tenant-scope —— kpiChangeRate 必须按 actor 限定范围</li>
 *   <li>Bug#13 中危：gate-mismatch —— sign() 签名含 actorId 防同角色多账号混淆</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class RequirementChangeSecurityRound2Test {

    private static final IpdActor MARKET_PM = new IpdActor(101L, "mkt", "MARKET_PM", 11L);
    private static final IpdActor RD_PM = new IpdActor(202L, "rd", "RD_PM", 11L);
    private static final IpdActor MARKET_PM_OTHER = new IpdActor(303L, "mkt2", "MARKET_PM", 22L);

    @Mock
    private RequirementChangeMapper changeMapper;
    @Mock
    private RequirementMapper reqMapper;
    @Mock
    private AuditLogService auditLogService;

    private RequirementChangeService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RequirementChange.class);
        TableInfoHelper.initTableInfo(assistant, Requirement.class);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(auditLogService.append(any(AuditLog.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        service = new RequirementChangeService(changeMapper, reqMapper, auditLogService);
    }

    private RequirementChange pendingChange(String signatures) {
        RequirementChange c = new RequirementChange();
        c.setId(99L);
        c.setRequirementId(7L);
        c.setProjectId(33L);
        c.setStatus("PENDING_SIGN");
        c.setSignatures(signatures);
        return c;
    }

    // ==================== Bug#13：gate-mismatch 签名格式 ====================

    @Test
    @DisplayName("Bug#13: sign() 签名格式 " +
        "MARKET_PM:<actorId>=APPROVE（防同角色多账号混淆）")
    void sign_signatureContainsActorId() {
        RequirementChange c = pendingChange(null);
        when(changeMapper.selectById(99L)).thenReturn(c);

        RequirementChange out = service.sign(99L, "APPROVE", "ok", MARKET_PM);

        assertThat(out.getSignatures()).contains("MARKET_PM:101=APPROVE");
    }

    @Test
    @DisplayName("Bug#13: 同 actor 重签被拒绝（重复签名检测）")
    void sign_duplicateRejected() {
        RequirementChange c = pendingChange("MARKET_PM:101=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);

        assertThatThrownBy(() -> service.sign(99L, "APPROVE", "again", MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("Bug#13: 双签后状态为 APPROVED（签名含 actorId 仍能被 hasMarket/hasRd 识别）")
    void sign_doubleApproveSucceeds() {
        RequirementChange c = pendingChange("MARKET_PM:101=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).status("SUBMITTED").build());

        RequirementChange out = service.sign(99L, "APPROVE", "ok", RD_PM);

        assertThat(out.getStatus()).isEqualTo("APPROVED");
        assertThat(out.getSignatures()).contains("MARKET_PM:101=APPROVE");
        assertThat(out.getSignatures()).contains("RD_PM:202=APPROVE");
    }

    @Test
    @DisplayName("Bug#13: 不同 actor 同角色双签可累计（actorId 区分，避免单人冒充双签）")
    void sign_differentActorSameRoleAccumulates() {
        // 第一位 MARKET_PM 签 101
        RequirementChange c = pendingChange("MARKET_PM:101=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);

        // 第二位 MARKET_PM 303 应被允许再签（不同 actorId）—— 累积为 partial_sign
        RequirementChange out = service.sign(99L, "APPROVE", "ok", MARKET_PM_OTHER);

        assertThat(out.getStatus()).isEqualTo("PENDING_SIGN"); // 仍待 RD_PM
        assertThat(out.getSignatures()).contains("MARKET_PM:101=APPROVE");
        assertThat(out.getSignatures()).contains("MARKET_PM:303=APPROVE");
    }

    // ==================== Bug#10：state-drift ====================

    @Test
    @DisplayName("Bug#10: applyApprovedToRequirement 失败 ⇒ RuntimeException 向上传播，change 不变 APPROVED")
    void sign_writeBackFailurePropagates() {
        RequirementChange c = pendingChange("MARKET_PM:101=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).status("SUBMITTED").build());
        doThrow(new RuntimeException("DB 连接失败")).when(reqMapper).updateById(any(Requirement.class));

        assertThatThrownBy(() -> service.sign(99L, "APPROVE", "ok", RD_PM))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB 连接失败");

        // 失败审计独立落库（AuditLogService.append REQUIRES_NEW）
        verify(auditLogService, atLeastOnce()).append(any(AuditLog.class));
        // change 状态没变 APPROVED（无 updateById 写入 —— 因为 transaction rollback）
        verify(changeMapper, never()).updateById(any(RequirementChange.class));
    }

    // ==================== Bug#11：sensitive-observability ====================

    @Test
    @DisplayName("Bug#11: 失败审计 reason 仅含异常类名（不含 ex.getMessage）")
    void sign_failureAuditContainsOnlyClassName() {
        RequirementChange c = pendingChange("MARKET_PM:101=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).status("SUBMITTED").build());
        doThrow(new RuntimeException("SQL: select * from secret_table where x='PII'"))
            .when(reqMapper).updateById(any(Requirement.class));

        try {
            service.sign(99L, "APPROVE", "ok", RD_PM);
        } catch (RuntimeException ignored) {
            // 期望抛出
        }

        // 校验至少有一个审计包含 "RuntimeException" 但不含 "secret_table" / "PII"
        org.mockito.ArgumentCaptor<AuditLog> cap = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(cap.capture());
        boolean foundFailureAudit = cap.getAllValues().stream()
            .anyMatch(log -> log.getAction() != null && log.getAction().contains("WRITE_BACK_FAIL")
                && log.getReason() != null && log.getReason().contains("RuntimeException")
                && !log.getReason().contains("secret_table")
                && !log.getReason().contains("PII"));
        assertThat(foundFailureAudit)
            .as("失败审计 reason 仅含类名，不含敏感 SQL/PII 内容")
            .isTrue();
    }

    // ==================== Bug#12：missing-tenant-scope ====================

    @Test
    @DisplayName("Bug#12: kpiChangeRate(null) 抛 UNAUTHORIZED（拒绝 null 旁路）")
    void kpiChangeRate_rejectsNullActor() {
        assertThatThrownBy(() -> service.kpiChangeRate(null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bug#12: 非超管无 groupId 调用 kpiChangeRate 被拒绝")
    void kpiChangeRate_nonSuperAdminRequiresGroupId() {
        IpdActor noGroup = new IpdActor(1L, "x", "MARKET_PM", null);
        assertThatThrownBy(() -> service.kpiChangeRate(noGroup))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("Bug#12: 非超管调用 kpiChangeRate 限定本组（应用层 apply 过滤）")
    void kpiChangeRate_nonSuperAdminAppliesGroupScope() {
        when(changeMapper.selectCount(any())).thenReturn(0L);
        when(reqMapper.selectCount(any())).thenReturn(10L);

        double rate = service.kpiChangeRate(MARKET_PM);

        assertThat(rate).isEqualTo(0.0);
        // 验证至少有一次 selectCount 调用
        verify(changeMapper, atLeastOnce()).selectCount(any());
    }

    @Test
    @DisplayName("Bug#12: 超管调用 kpiChangeRate 不限定 groupId（全局视角）")
    void kpiChangeRate_superAdminGlobal() {
        IpdActor admin = new IpdActor(1L, "admin", "SUPER_ADMIN", null);
        when(changeMapper.selectCount(any())).thenReturn(3L);
        when(reqMapper.selectCount(any())).thenReturn(10L);

        double rate = service.kpiChangeRate(admin);

        assertThat(rate).isEqualTo(0.3);
    }
}
