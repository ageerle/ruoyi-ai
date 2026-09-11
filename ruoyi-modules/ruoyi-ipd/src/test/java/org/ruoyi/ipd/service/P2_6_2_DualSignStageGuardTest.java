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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-6.2 需求变更双签回写与阶段门禁（AC-REQ-08 / AC-GATE-11）。
 *
 * <p>聚焦三件事（与 P2-6.1 边界清晰）：
 * <ul>
 *   <li><b>双签回写</b>：sign() 后 signatures 字段必须正确累积（"MARKET_PM:&lt;id&gt;=APPROVE;RD_PM:&lt;id&gt;=APPROVE"），
 *       含 actorId 防同角色多账号混淆（接续 SEC-REV-REQ-CHANGE-04）</li>
 *   <li><b>阶段门禁</b>：advanceStage() 跳阶前查询 hasOpenChange，存在未闭环变更单 ⇒ STATE_CONFLICT（409）</li>
 *   <li><b>影响快照必填</b>：create() 时 beforeSnapshot / afterSnapshot 必须为合法 JSON 且
 *       包含范围/成本/时限/质量四维度（缺失 ⇒ PARAM_INVALID）</li>
 * </ul>
 *
 * <p>与 P2-6.1 的边界：本卡只验「回写正确 + 门禁拒绝 + 维度必填」三件事，
 * 状态机迁移、kpiChangeRate、回写失败 rollback 等已有 SEC-REV-round2 覆盖。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P2_6_2_DualSignStageGuardTest {

    private static final IpdActor MARKET_PM = new IpdActor(12L, "alice", "MARKET_PM", 11L);
    private static final IpdActor RD_PM = new IpdActor(34L, "bob", "RD_PM", 11L);
    private static final IpdActor GROUP_LEADER = new IpdActor(99L, "leader", "GROUP_LEADER", 11L);
    private static final IpdActor CREATOR_PM = new IpdActor(7L, "creator", "MARKET_PM", 11L);

    private static final String FULL_SNAPSHOT_BEFORE = "{"
        + "\"范围\":\"原方案只覆盖线上单门店\","
        + "\"成本\":\"开发成本 30 万\","
        + "\"时限\":\"原计划 T+30 上线\","
        + "\"质量\":\"P1 缺陷率 < 0.5%\""
        + "}";
    private static final String FULL_SNAPSHOT_AFTER = "{"
        + "\"范围\":\"新增线下 5 个门店\","
        + "\"成本\":\"开发成本追加至 45 万\","
        + "\"时限\":\"顺延至 T+45 上线\","
        + "\"质量\":\"P1 缺陷率保持 < 0.5%\""
        + "}";

    @Mock private RequirementChangeMapper changeMapper;
    @Mock private RequirementMapper reqMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMapper projectMapper;
    @Mock private org.ruoyi.ipd.mapper.StageActionMapper stageActionMapper;
    @Mock private org.ruoyi.ipd.mapper.KpiRecordMapper kpiRecordMapper;
    @Mock private GateEngine gateEngine;
    @Mock private ProjectBootstrapService projectBootstrapService;
    @Mock private ProjectCertService projectCertService;
    @Mock private RequirementChangeService requirementChangeService;

    private RequirementChangeService reqChangeService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RequirementChange.class);
        TableInfoHelper.initTableInfo(assistant, Requirement.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(auditLogService.append(any(AuditLog.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        reqChangeService = new RequirementChangeService(changeMapper, reqMapper, auditLogService);
    }

    /** 构造一个 PENDING_SIGN 状态的变更单（含四维度快照）。 */
    private RequirementChange pendingChange(String signatures) {
        RequirementChange c = new RequirementChange();
        c.setId(99L);
        c.setRequirementId(7L);
        c.setProjectId(33L);
        c.setStatus("PENDING_SIGN");
        c.setSignatures(signatures);
        c.setChangeType("SCOPE");
        c.setReason("扩大门店覆盖");
        c.setBeforeSnapshot(FULL_SNAPSHOT_BEFORE);
        c.setAfterSnapshot(FULL_SNAPSHOT_AFTER);
        return c;
    }

    /** 构造一个 ACTIVE / CONCEPT 阶段的项目。 */
    private Project activeConcept(Long id, Long mainGroupId) {
        Project p = new Project();
        p.setId(id);
        p.setName("p2-6-2-fixture");
        p.setStatus("ACTIVE");
        p.setCurrentStage("CONCEPT");
        p.setMainGroupId(mainGroupId);
        p.setDelFlag("0");
        return p;
    }

    // ==================== 维度 1：双签累积 ====================

    @Test
    @DisplayName("双签累积：MARKET_PM 先签 → signatures 含 'MARKET_PM:12=APPROVE'，状态保持 PENDING_SIGN")
    void sign_marketPmFirst_partialSignatures() {
        RequirementChange c = pendingChange(null);
        when(changeMapper.selectById(99L)).thenReturn(c);

        RequirementChange out = reqChangeService.sign(99L, "APPROVE", "ok", MARKET_PM);

        assertThat(out.getSignatures()).contains("MARKET_PM:12=APPROVE");
        assertThat(out.getStatus()).isEqualTo("PENDING_SIGN"); // 等 RD_PM
    }

    @Test
    @DisplayName("双签累积：RD_PM 后签 → status=APPROVED + signatures 同时含 MARKET_PM:12 与 RD_PM:34")
    void sign_rdPmSecond_statusApproveAndSignaturesComplete() {
        RequirementChange c = pendingChange("MARKET_PM:12=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).status("SUBMITTED").build());

        RequirementChange out = reqChangeService.sign(99L, "APPROVE", "ok", RD_PM);

        assertThat(out.getStatus()).isEqualTo("APPROVED");
        // 累积格式精确断言（与 P2-6.1 一致）
        assertThat(out.getSignatures()).contains("MARKET_PM:12=APPROVE");
        assertThat(out.getSignatures()).contains("RD_PM:34=APPROVE");
        assertThat(out.getSignatures()).isEqualTo("MARKET_PM:12=APPROVE;RD_PM:34=APPROVE");
    }

    // ==================== 维度 2：重复签名拒绝 ====================

    @Test
    @DisplayName("重复签名：MARKET_PM 同 actor 二签 → STATE_CONFLICT(409)")
    void sign_duplicateBySameActorRejected() {
        RequirementChange c = pendingChange("MARKET_PM:12=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);

        assertThatThrownBy(() -> reqChangeService.sign(99L, "APPROVE", "again", MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    // ==================== 维度 3：跨组守卫（仅 MARKET_PM / RD_PM 可签） ====================

    @Test
    @DisplayName("跨组守卫：GROUP_LEADER 调用 sign() → FORBIDDEN(403)")
    void sign_nonSignerRoleRejected() {
        // 角色守卫在 read 前即拒，changeMapper.selectById 不会被调用 —— 用 lenient 避免 strict-stubbing 报错
        org.mockito.Mockito.lenient()
            .when(changeMapper.selectById(99L)).thenReturn(pendingChange(null));

        assertThatThrownBy(() -> reqChangeService.sign(99L, "APPROVE", "x", GROUP_LEADER))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    // ==================== 维度 4：影响快照必填（create 阶段四维度校验） ====================

    @Test
    @DisplayName("影响快照必填：afterSnapshot 缺「范围」维度 → PARAM_INVALID(400)")
    void create_snapshotMissingDimensionRejected() {
        // 故意缺「范围」
        String afterMissingScope = "{"
            + "\"成本\":\"45 万\","
            + "\"时限\":\"T+45\","
            + "\"质量\":\"P1<0.5%\""
            + "}";
        RequirementChange req = new RequirementChange();
        req.setChangeType("SCOPE");
        req.setReason("扩大覆盖");
        req.setRequirementId(7L);
        req.setBeforeSnapshot(FULL_SNAPSHOT_BEFORE);
        req.setAfterSnapshot(afterMissingScope);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).projectId(33L).build());

        assertThatThrownBy(() -> reqChangeService.create(req, CREATOR_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("影响快照必填：afterSnapshot 为 null → PARAM_INVALID(400)")
    void create_snapshotNullRejected() {
        RequirementChange req = new RequirementChange();
        req.setChangeType("SCOPE");
        req.setReason("扩大覆盖");
        req.setRequirementId(7L);
        req.setBeforeSnapshot(FULL_SNAPSHOT_BEFORE);
        req.setAfterSnapshot(null);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).projectId(33L).build());

        assertThatThrownBy(() -> reqChangeService.create(req, CREATOR_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("影响快照必填：四维度齐全 → create 成功落库")
    void create_snapshotCompleteSuccess() {
        RequirementChange req = new RequirementChange();
        req.setChangeType("SCOPE");
        req.setReason("扩大覆盖");
        req.setRequirementId(7L);
        req.setBeforeSnapshot(FULL_SNAPSHOT_BEFORE);
        req.setAfterSnapshot(FULL_SNAPSHOT_AFTER);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).projectId(33L).build());

        RequirementChange out = reqChangeService.create(req, CREATOR_PM);

        assertThat(out.getStatus()).isEqualTo("DRAFT");
        assertThat(out.getProjectId()).isEqualTo(33L);
        verify(changeMapper, atLeastOnce()).insert(any(RequirementChange.class));
    }

    // ==================== 维度 5：阶段门禁 —— 存在 PENDING_SIGN ⇒ 拒绝 ====================

    @Test
    @DisplayName("阶段门禁：advanceStage 时存在未闭环变更单 → STATE_CONFLICT + STAGE_GUARD_BLOCKED 审计")
    void advanceStage_openChangeBlocks() {
        // 1) ProjectService.advanceStage 入参
        Project project = activeConcept(33L, 11L);
        when(projectMapper.selectById(33L)).thenReturn(project);
        when(requirementChangeService.hasOpenChange(33L)).thenReturn(true);
        when(requirementChangeService.countOpenByProject(33L)).thenReturn(1);

        ProjectService projectService = new ProjectService(
            projectMapper,
            org.mockito.Mockito.mock(org.ruoyi.ipd.mapper.ProductMapper.class),
            stageActionMapper, kpiRecordMapper,
            auditLogService, gateEngine,
            projectBootstrapService, projectCertService,
            NoopTransactionManager.INSTANCE,
            requirementChangeService);

        // 2) 拒绝断言
        assertThatThrownBy(() -> projectService.advanceStage(33L, 5L, 11L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        // 3) STAGE_GUARD_BLOCKED 审计落库（reason=项目名，beforeData 含 openChangeCount）
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(cap.capture());
        boolean hasGuardBlocked = cap.getAllValues().stream()
            .anyMatch(log -> "STAGE_GUARD_BLOCKED".equals(log.getAction())
                && log.getEntityId() != null && log.getEntityId().equals(33L)
                && log.getBeforeData() != null && log.getBeforeData().contains("openChangeCount")
                && log.getReason() != null && log.getReason().contains("p2-6-2-fixture"));
        assertThat(hasGuardBlocked)
            .as("阶段门禁拒绝必须落 STAGE_GUARD_BLOCKED 审计（含 prior + attemptedNext + openChangeCount）")
            .isTrue();

        // 4) 项目 currentStage 不被推进
        assertThat(project.getCurrentStage()).isEqualTo("CONCEPT");
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    // ==================== 维度 6：阶段门禁 —— 无未闭环 ⇒ 允许推进 ====================

    @Test
    @DisplayName("阶段门禁：advanceStage 时无未闭环变更单 → 正常推进 CONCEPT→PLAN")
    void advanceStage_noOpenChangeAllows() {
        Project project = activeConcept(34L, 11L);
        when(projectMapper.selectById(34L)).thenReturn(project);
        when(requirementChangeService.hasOpenChange(34L)).thenReturn(false);

        ProjectService projectService = new ProjectService(
            projectMapper,
            org.mockito.Mockito.mock(org.ruoyi.ipd.mapper.ProductMapper.class),
            stageActionMapper, kpiRecordMapper,
            auditLogService, gateEngine,
            projectBootstrapService, projectCertService,
            NoopTransactionManager.INSTANCE,
            requirementChangeService);

        Project out = projectService.advanceStage(34L, 5L, 11L, "MARKET_PM");

        assertThat(out.getCurrentStage()).isEqualTo("PLAN");
        verify(gateEngine, atLeastOnce()).check(any(Project.class), any());
        // 成功路径写 STAGE 推进审计（不是 STAGE_GUARD_BLOCKED）
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(cap.capture());
        boolean noGuardBlocked = cap.getAllValues().stream()
            .noneMatch(log -> "STAGE_GUARD_BLOCKED".equals(log.getAction()));
        assertThat(noGuardBlocked).isTrue();
    }

    // ==================== 维度 7：reject 路径（任一 REJECT ⇒ REJECTED + 签名累积） ====================

    @Test
    @DisplayName("reject 路径：MARKET_PM REJECT → status=REJECTED + signatures 含 'MARKET_PM:12=REJECT'")
    void sign_rejectPath_stateRejectedAndSignaturesAccumulated() {
        RequirementChange c = pendingChange(null);
        when(changeMapper.selectById(99L)).thenReturn(c);

        RequirementChange out = reqChangeService.sign(99L, "REJECT", "成本不可控", MARKET_PM);

        assertThat(out.getStatus()).isEqualTo("REJECTED");
        assertThat(out.getSignatures()).contains("MARKET_PM:12=REJECT");

        // REJECT 路径不写需求池（避免误落 ADOPTED 状态）
        verify(reqMapper, never()).updateById(any(Requirement.class));
        // REJECT 路径不调用 GateEngine / 跳阶相关逻辑
        verify(requirementChangeService, never()).hasOpenChange(anyLong());
    }

    // ==================== 防御性：提交后再校验四维度（submit 兜底） ====================

    @Test
    @DisplayName("submit() 兜底：create 后 snapshot 被清空 → submit 拒绝 PARAM_INVALID")
    void submit_snapshotTamperedAfterCreate() {
        RequirementChange c = pendingChange(null);
        c.setStatus("DRAFT");
        c.setCreateBy(CREATOR_PM.id());
        c.setBeforeSnapshot(null); // 模拟 create 之后被篡改
        c.setAfterSnapshot(null);
        when(changeMapper.selectById(99L)).thenReturn(c);

        assertThatThrownBy(() -> reqChangeService.submit(99L, CREATOR_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    // ==================== 防御性：applyApprovedToRequirement 失败不应让 change 进入 APPROVED ====================

    @Test
    @DisplayName("并发防御：sign() 写需求池失败 → RuntimeException 传播，change 不变 APPROVED（接续 SEC-REV-REQ-CHANGE-01）")
    void sign_concurrentWriteBackFailure_propagates() {
        RequirementChange c = pendingChange("MARKET_PM:12=APPROVE");
        when(changeMapper.selectById(99L)).thenReturn(c);
        when(reqMapper.selectById(7L)).thenReturn(Requirement.builder().id(7L).status("SUBMITTED").build());
        doThrow(new RuntimeException("DB 写需求池失败"))
            .when(reqMapper).updateById(any(Requirement.class));

        assertThatThrownBy(() -> reqChangeService.sign(99L, "APPROVE", "ok", RD_PM))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB 写需求池失败");

        // 失败审计落库 + change status 未置 APPROVED（无 updateById 写入）
        verify(auditLogService, atLeastOnce()).append(any(AuditLog.class));
        verify(changeMapper, never()).updateById(any(RequirementChange.class));
    }
}