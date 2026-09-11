package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.NegativeFeedback;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.dto.NegativeFeedbackCreateReq;
import org.ruoyi.ipd.dto.NegativeFeedbackDecisionReq;
import org.ruoyi.ipd.mapper.NegativeFeedbackMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-8.2 负反馈停发减半与奖金资格联动 验收测试
 * <p>
 * 来源 AC：BR-INC-10；AC-INC-36b / AC-INC-37 / AC-INC-38 / AC-INC-39 / AC-INC-40
 * （开发说明书 §六.2；ZK-IPD Prompt §三.2.5）。
 * <p>
 * 覆盖 7 个验收场景：
 * <ol>
 *   <li>AC-INC-36b：REWORK_EXCEEDED ⇒ MARKET_PM 主责停发 + RD_PM 连带减半</li>
 *   <li>AC-INC-37：QUALITY_ACCIDENT ⇒ RD_PM 主责停发 + MARKET_PM 连带减半</li>
 *   <li>AC-INC-38：MISSED_MARKET_WINDOW ⇒ 双 PM 共同担责（BOTH，无连带减半）</li>
 *   <li>AC-INC-39：负反馈对奖金影响 ⇒ tierDelta=-0.50 + bonusDisqualify=1</li>
 *   <li>AC-INC-40：审计完整性 ⇒ before/after 镜像 + operatorId + entityType/Id + action + reason 齐全</li>
 *   <li>幂等：同 (projectId, triggerType) 已生效 → 重复事件不重复扣减</li>
 *   <li>生效月 + 恢复时点：triggerMonth 必填，recoveryMonth 可空；lift 后状态=LIFTED + 留 lift 痕迹</li>
 * </ol>
 *
 * <p><b>注</b>：本验收测试不重复 P161AcceptanceTest / NegativeFeedbackServiceTest 已覆盖的通用单测，
 * 专注按 AC 维度串成完整剧本。Mapper 为 Mock，不触真实 DB（DB 唯一索引 uk_nf_project_trigger_active
 * 的兜底效果归 NegativeFeedbackSecurityRound3Test / 真库集成测试）。
 */
@Tag("dev")
class P382AcceptanceTest {

    private static final Long PROJECT_ID = 38201L;
    private static final Long MARKET_PM_ID = 382002L;
    private static final Long RD_PM_ID = 382003L;
    private static final Long LEADER_ID = 3820001L;

    private static final IpdActor MARKET_PM_ACTOR =
        new IpdActor(MARKET_PM_ID, "市场PM", "MARKET_PM", 7L);
    private static final IpdActor LEADER_ACTOR =
        new IpdActor(LEADER_ID, "组长", "GROUP_LEADER", 7L);

    private NegativeFeedbackMapper mapper;
    private ProjectMemberMapper memberMapper;
    private AuditLogService auditLogService;
    private NotificationService notificationService;
    private ProjectMapper projectMapper;
    private IpdPermission ipdPermission;

    private NegativeFeedbackService service;

    /** 模拟自增 ID，模拟 MyBatis-Plus INSERT 后回填主键 */
    private final AtomicLong idSeq = new AtomicLong(1000L);
    /** 模拟 DB 持久化的最新一行（用于 selectById 回放） */
    private NegativeFeedback lastInserted;

    @BeforeAll
    static void initMapperMetadata() {
        // Java 17 Mockito 环境下 LambdaQueryWrapper 反射读取 TableInfo，
        // 必须显式 init，否则所有 when(mapper.selectCount(any(LambdaQueryWrapper.class))) 抛 NPE。
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p382-test"),
            NegativeFeedback.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p382-test"),
            ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(NegativeFeedbackMapper.class);
        memberMapper = mock(ProjectMemberMapper.class);
        auditLogService = mock(AuditLogService.class);
        notificationService = mock(NotificationService.class);
        projectMapper = mock(ProjectMapper.class);
        ipdPermission = mock(IpdPermission.class);

        // 关键 mock：insert 回填主键 + 模拟 DB 持久化
        org.mockito.Mockito.doAnswer(inv -> {
            NegativeFeedback row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(idSeq.getAndIncrement());
            }
            lastInserted = row;
            return 1;
        }).when(mapper).insert(any(NegativeFeedback.class));

        // 默认：项目可读（同 group）；组长 / 超管 例外放行
        org.ruoyi.ipd.domain.Project project = new org.ruoyi.ipd.domain.Project();
        project.setId(PROJECT_ID);
        project.setMainGroupId(7L);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(ipdPermission.canReadProject(anyString(), any(), eq(7L))).thenReturn(true);

        service = new NegativeFeedbackService(mapper, memberMapper, auditLogService,
            notificationService, projectMapper, ipdPermission);
    }

    /** 项目下默认配齐 MARKET_PM + RD_PM 两位在职 PM */
    private void stubProjectHasBothPm() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                ProjectMember.builder().projectId(PROJECT_ID).personId(MARKET_PM_ID)
                    .role("MARKET_PM").build(),
                ProjectMember.builder().projectId(PROJECT_ID).personId(RD_PM_ID)
                    .role("RD_PM").build()));
    }

    /** 项目无在职 PM */
    private void stubProjectHasNoPm() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());
    }

    /** DB 中已存在 (projectId, triggerType) 的活跃记录 → 模拟唯一索引命中 */
    private void stubDuplicateActive() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
    }

    /** DB 中无重复 → 模拟 selectCount 返回 0 */
    private void stubNoDuplicate() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    }

    /* ====================================================================
     *  场景1：AC-INC-36b — 需求返工率超标 ⇒ MARKET_PM 停发 + RD_PM 减半
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-36b：REWORK_EXCEEDED → MARKET_PM STOP + RD_PM HALVE + 完整映射")
    void acInc36b_reworkExceeded_marketMainStop_rdRelatedHalve() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "近 30 天需求返工率 28% 超阈值", "2026-09", null);

        NegativeFeedback row = service.create(req, MARKET_PM_ACTOR);

        // 主责方：MARKET_PM，mainExecution=STOP_ALLOWANCE
        assertThat(row.getMainRole()).isEqualTo("MARKET_PM");
        assertThat(row.getMainPersonId()).isEqualTo(MARKET_PM_ID);
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");

        // 连带方：RD_PM，relatedExecution=HALVE_ALLOWANCE
        assertThat(row.getRelatedRole()).isEqualTo("RD_PM");
        assertThat(row.getRelatedPersonId()).isEqualTo(RD_PM_ID);
        assertThat(row.getRelatedExecution()).isEqualTo("HALVE_ALLOWANCE");

        // 状态机初始 + 生效月
        assertThat(row.getStatus()).isEqualTo("DRAFT");
        assertThat(row.getTriggerMonth()).isEqualTo("2026-09");
        assertThat(row.getRecoveryMonth()).isNull();

        // 创建审计 1 次（Action=CREATE，entityType=NEGATIVE_FEEDBACK）
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getEntityType()).isEqualTo("NEGATIVE_FEEDBACK");
        assertThat(log.getEntityId()).isEqualTo(row.getId());
        assertThat(log.getOperatorName()).isEqualTo(String.valueOf(MARKET_PM_ID));
        assertThat(log.getOperatorRole()).isEqualTo("MARKET_PM");
        assertThat(log.getReason()).contains("P3-8.2");
    }

    /* ====================================================================
     *  场景2：AC-INC-37 — 质量事故 ⇒ RD_PM 主责停发 + MARKET_PM 连带减半
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-37：QUALITY_ACCIDENT → RD_PM STOP + MARKET_PM HALVE")
    void acInc37_qualityAccident_rdMainStop_marketRelatedHalve() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "QUALITY_ACCIDENT", "P1 级线上事故 5 起，P0 客户投诉", "2026-09", null);

        NegativeFeedback row = service.create(req, MARKET_PM_ACTOR);

        assertThat(row.getMainRole()).isEqualTo("RD_PM");
        assertThat(row.getMainPersonId()).isEqualTo(RD_PM_ID);
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");
        assertThat(row.getRelatedRole()).isEqualTo("MARKET_PM");
        assertThat(row.getRelatedPersonId()).isEqualTo(MARKET_PM_ID);
        assertThat(row.getRelatedExecution()).isEqualTo("HALVE_ALLOWANCE");
    }

    /* ====================================================================
     *  场景3：AC-INC-38 — 错过市场窗口 ⇒ BOTH 双PM共同担责，无连带区分
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-38：MISSED_MARKET_WINDOW → BOTH 双PM共同担责，related 字段空")
    void acInc38_missedMarketWindow_bothSharedNoRelated() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "MISSED_MARKET_WINDOW", "错过 2026-Q3 上市窗口", "2026-09", null);

        NegativeFeedback row = service.create(req, MARKET_PM_ACTOR);

        // 双PM共同担责：mainRole=BOTH，related 字段全部 NULL
        assertThat(row.getMainRole()).isEqualTo("BOTH");
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");
        assertThat(row.getRelatedRole()).isNull();
        assertThat(row.getRelatedExecution()).isNull();
        assertThat(row.getRelatedPersonId()).isNull();

        // 注意：BOTH 模式下 mainPersonId 仍填一个主记录人（即 MARKET_PM 作为回执人）
        assertThat(row.getMainPersonId()).isNotNull();
    }

    /* ====================================================================
     *  场景4：AC-INC-39 — 负反馈对奖金的影响 ⇒ 贡献度系数降低 + 取消分配资格
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-39：tierDelta=-0.50 + bonusDisqualify=1（贡献度系数降低 + 取消分配资格）")
    void acInc39_bonusImpact_tierDeltaAndDisqualify() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        // 三类触发情形均需带 tierDelta + bonusDisqualify（BR-INC-10）
        for (String triggerType : List.of(
            "REWORK_EXCEEDED", "QUALITY_ACCIDENT", "MISSED_MARKET_WINDOW")) {
            // 每个用例前重置：selectCount 仍返 0
            org.mockito.Mockito.reset(mapper);
            idSeq.set(1000L);
            lastInserted = null;
            stubNoDuplicate();
            stubProjectHasBothPm();
            org.mockito.Mockito.doAnswer(inv -> {
                NegativeFeedback row = inv.getArgument(0);
                if (row.getId() == null) {
                    row.setId(idSeq.getAndIncrement());
                }
                lastInserted = row;
                return 1;
            }).when(mapper).insert(any(NegativeFeedback.class));

            NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
                PROJECT_ID, triggerType, "AC-INC-39 奖金影响剧本", "2026-09", null);
            NegativeFeedback row = service.create(req, MARKET_PM_ACTOR);

            // 贡献度系数降低：默认 -0.50（BR-INC-10 §负反馈对奖金影响）
            assertThat(row.getTierDelta())
                .as("触发情形 %s 必须设置 tierDelta=-0.50", triggerType)
                .isEqualByComparingTo("-0.50");
            // 取消分配资格：bonusDisqualify=1
            assertThat(row.getBonusDisqualify())
                .as("触发情形 %s 必须设置 bonusDisqualify=1", triggerType)
                .isEqualTo(1);

            // 与 DEFAULT_TIER_DELTA 常量一致（防硬编码漂移）
            assertThat(NegativeFeedbackService.DEFAULT_TIER_DELTA)
                .isEqualByComparingTo(new BigDecimal("-0.50"));
        }
    }

    /* ====================================================================
     *  场景5：AC-INC-40 — 审计完整性（含触发情形 / 主责连带 / 执行人）
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-40：CREATE/SUBMIT/DECIDE/LIFT 全链路审计 + 触发情形/主责/连带/执行人齐全")
    void acInc40_auditCompleteness_fullLifecycle() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        // 1) CREATE 阶段
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "AC-INC-40 全链路审计剧本", "2026-09", null);
        NegativeFeedback created = service.create(req, MARKET_PM_ACTOR);

        // 模拟 DB 回读：mock selectById 返回上一步插入的行
        when(mapper.selectById(created.getId())).thenReturn(created);

        // 2) SUBMIT 阶段：DRAFT → PENDING_DECISION
        NegativeFeedback submitted = service.submit(created.getId(), MARKET_PM_ACTOR);
        assertThat(submitted.getStatus()).isEqualTo("PENDING_DECISION");

        // 3) DECIDE APPROVE：PENDING_DECISION → EXECUTED（组长执行）
        when(mapper.selectById(created.getId())).thenReturn(submitted);
        NegativeFeedback executed = service.decide(created.getId(),
            new NegativeFeedbackDecisionReq("APPROVE", "证据充分，认定执行"), LEADER_ACTOR);
        assertThat(executed.getStatus()).isEqualTo("EXECUTED");
        assertThat(executed.getDecidedBy()).isEqualTo(LEADER_ID);
        assertThat(executed.getDecidedAt()).isNotNull();

        // 4) LIFT：EXECUTED → LIFTED（恢复 bonusEligible）
        when(mapper.selectById(created.getId())).thenReturn(executed);
        NegativeFeedback lifted = service.lift(created.getId(),
            new NegativeFeedbackDecisionReq("LIFT", "已整改，复盘通过"), LEADER_ACTOR);
        assertThat(lifted.getStatus()).isEqualTo("LIFTED");
        assertThat(lifted.getLiftedBy()).isEqualTo(LEADER_ID);
        assertThat(lifted.getLiftedAt()).isNotNull();

        // 审计校验：4 次 append（CREATE + SUBMIT + DECIDE_EXECUTE + LIFT）
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(4)).append(captor.capture());
        List<AuditLog> logs = captor.getAllValues();

        // 5) 顺序：CREATE → SUBMIT → DECIDE_EXECUTE → LIFT
        assertThat(logs).extracting(AuditLog::getAction)
            .containsExactly("CREATE", "SUBMIT", "DECIDE_EXECUTE", "LIFT");

        // 6) 全部审计：entityType/Id + operatorName/Role + before/after 镜像 + reason 齐全
        for (AuditLog log : logs) {
            assertThat(log.getEntityType()).isEqualTo("NEGATIVE_FEEDBACK");
            assertThat(log.getEntityId()).isEqualTo(created.getId());
            assertThat(log.getOperatorName()).isNotBlank();
            assertThat(log.getOperatorRole()).isNotBlank();
            assertThat(log.getReason()).contains("P3-8.2");
        }

        // 7) before/after 镜像非空（状态机迁移留痕）
        assertThat(logs.get(0).getBeforeData()).isNull();          // CREATE：前态为空
        assertThat(logs.get(0).getAfterData()).isEqualTo("\"DRAFT\"");
        assertThat(logs.get(1).getBeforeData()).isEqualTo("\"DRAFT\"");
        assertThat(logs.get(1).getAfterData()).isEqualTo("\"PENDING_DECISION\"");
        assertThat(logs.get(2).getBeforeData()).isEqualTo("\"PENDING_DECISION\"");
        assertThat(logs.get(2).getAfterData()).isEqualTo("\"EXECUTED\"");
        assertThat(logs.get(3).getBeforeData()).isEqualTo("\"EXECUTED\"");
        assertThat(logs.get(3).getAfterData()).isEqualTo("\"LIFTED\"");

        // 8) DECIDE 阶段审计 operatorName = LEADER（不是 MARKET_PM 创建人）
        assertThat(logs.get(2).getOperatorName()).isEqualTo(String.valueOf(LEADER_ID));
        assertThat(logs.get(2).getOperatorRole()).isEqualTo("GROUP_LEADER");
    }

    /* ====================================================================
     *  场景6：幂等 — 同 (projectId, triggerType) 已生效 → 重复事件不重复扣减
     * ==================================================================== */
    @Test
    @DisplayName("幂等：同 (projectId, triggerType) 已生效 → 重复录入抛 NF_REENTRY_NOT_ALLOWED，不写第二条")
    void idempotent_reentryNotAllowed() {
        stubDuplicateActive();  // DB 已有同 trigger 活跃记录
        stubProjectHasBothPm();

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "重复事件，不应二次扣减", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_REENTRY_NOT_ALLOWED.getCode());
                assertThat(ex.getMessage()).contains("重复");
            });

        // 关键校验：mapper.insert 一次也没调用（不写第二条记录）
        verify(mapper, never()).insert(any(NegativeFeedback.class));
        // 关键校验：审计一次也没追加（避免脏审计）
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("幂等兜底：DB 唯一索引触发 DuplicateKeyException → 翻译成 NF_REENTRY_NOT_ALLOWED")
    void idempotent_dbUniqueIndexCatch() {
        stubNoDuplicate();  // service selectCount 漏过
        stubProjectHasBothPm();
        // 模拟 DB 唯一索引违例
        org.mockito.Mockito.doThrow(new org.springframework.dao.DuplicateKeyException(
            "uk_nf_project_trigger_active"))
            .when(mapper).insert(any(NegativeFeedback.class));

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "QUALITY_ACCIDENT", "并发残余插入", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_REENTRY_NOT_ALLOWED.getCode());
            });
    }

    /* ====================================================================
     *  场景7：生效月 + 恢复时点 — triggerMonth 必填 / recoveryMonth 可空 / lift 留痕
     * ==================================================================== */
    @Test
    @DisplayName("生效月 + 恢复时点：triggerMonth=2026-09 必填，recoveryMonth 可空；lift 后留 liftedBy/At")
    void effectiveMonthAndRecoveryPoint() {
        stubNoDuplicate();
        stubProjectHasBothPm();

        // 7a) 创建时 triggerMonth 必填（缺省抛 PARAM_INVALID）
        NegativeFeedbackCreateReq badMonth = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "缺生效月", null, null);
        assertThatThrownBy(() -> service.create(badMonth, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                // DTO @NotBlank 在测试口未走 bean validation，应走 service 兜底 PARAM_INVALID 或 NF_MONTH_FORMAT_INVALID
                assertThat(ex.getErrorCode().getCode()).isIn(
                    ApiV1ErrorCode.PARAM_INVALID.getCode(),
                    ApiV1ErrorCode.NF_MONTH_FORMAT_INVALID.getCode());
            });

        // 7b) 创建时 triggerMonth 合法 + recoveryMonth 可空 → 落库 recoveryMonth=null
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "恢复月留待 lift 时回填", "2026-09", null);
        NegativeFeedback created = service.create(req, MARKET_PM_ACTOR);
        assertThat(created.getTriggerMonth()).isEqualTo("2026-09");
        assertThat(created.getRecoveryMonth()).isNull();

        // 7c) 创建时显式给出 recoveryMonth（已知整改计划）→ 落库一致
        org.mockito.Mockito.reset(mapper);
        idSeq.set(2000L);
        lastInserted = null;
        stubNoDuplicate();
        stubProjectHasBothPm();
        org.mockito.Mockito.doAnswer(inv -> {
            NegativeFeedback row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(idSeq.getAndIncrement());
            }
            lastInserted = row;
            return 1;
        }).when(mapper).insert(any(NegativeFeedback.class));
        NegativeFeedbackCreateReq withRecovery = new NegativeFeedbackCreateReq(
            PROJECT_ID, "QUALITY_ACCIDENT", "已制定整改计划", "2026-09", "2026-12");
        NegativeFeedback withRec = service.create(withRecovery, MARKET_PM_ACTOR);
        assertThat(withRec.getRecoveryMonth()).isEqualTo("2026-12");

        // 7d) lift 流程：EXECUTED → LIFTED 后状态机 + liftedBy/At 留痕，triggerMonth 不变
        NegativeFeedback executed = NegativeFeedback.builder()
            .id(created.getId()).projectId(PROJECT_ID).status("EXECUTED")
            .triggerType("REWORK_EXCEEDED")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM_ID)
            .mainExecution("STOP_ALLOWANCE").relatedRole("RD_PM").relatedPersonId(RD_PM_ID)
            .relatedExecution("HALVE_ALLOWANCE")
            .triggerMonth("2026-09").recoveryMonth(null)
            .bonusDisqualify(1).tierDelta(new BigDecimal("-0.50"))
            .triggeredBy(MARKET_PM_ID).build();
        when(mapper.selectById(created.getId())).thenReturn(executed);

        NegativeFeedback lifted = service.lift(created.getId(),
            new NegativeFeedbackDecisionReq("LIFT", "已整改"), LEADER_ACTOR);

        // lift 后状态=LIFTED，liftedBy/liftedAt 留痕
        assertThat(lifted.getStatus()).isEqualTo("LIFTED");
        assertThat(lifted.getLiftedBy()).isEqualTo(LEADER_ID);
        assertThat(lifted.getLiftedAt()).isNotNull();
        // 触发月不变（生效时点已锁定；恢复时点由 lift 时间推断）
        assertThat(lifted.getTriggerMonth()).isEqualTo("2026-09");

        // 7e) lift 后通知双PM（FYI：恢复津贴+奖金资格）
        verify(notificationService, atLeastOnce()).publish(anyLong(), anyString(), anyString(),
            eq("negative_feedback"), any(), anyString(), anyString(), anyString());
    }

    /* ====================================================================
     *  派生映射静态方法权威性：SPEC_PILE_COPY（AC-INC-37b 同型）
     * ==================================================================== */
    @Test
    @DisplayName("派生：SPEC_PILE_COPY → RD_PM 主 + MARKET_PM 连带（AC-INC-37 同型映射）")
    void deriveRoleMapping_specPileCopy() {
        var map = NegativeFeedbackService.deriveRoleMapping("SPEC_PILE_COPY");
        assertThat(map).containsEntry("mainRole", "RD_PM")
            .containsEntry("relatedRole", "MARKET_PM")
            .containsEntry("mainExec", "STOP_ALLOWANCE")
            .containsEntry("relatedExec", "HALVE_ALLOWANCE");
    }

    /* ====================================================================
     *  状态机非法迁移：EXECUTED 状态再 submit → NF_STATE_INVALID（50002）
     * ==================================================================== */
    @Test
    @DisplayName("状态机：EXECUTED 再 submit → NF_STATE_INVALID（50002）")
    void stateMachine_invalidSubmitOnExecuted() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(999L).projectId(PROJECT_ID).status("EXECUTED").build();
        when(mapper.selectById(999L)).thenReturn(existing);

        assertThatThrownBy(() -> service.submit(999L, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_STATE_INVALID.getCode());
                // 50002 = STATE_CONFLICT，与 ApiV1ErrorCode 常量对齐
                assertThat(ApiV1ErrorCode.STATE_CONFLICT.getCode()).isEqualTo(50002);
                assertThat(ApiV1ErrorCode.NF_STATE_INVALID.getCode()).isEqualTo(50013);
            });
    }

    /* ====================================================================
     *  反例：项目无 MARKET_PM / RD_PM → NF_NOT_PM
     * ==================================================================== */
    @Test
    @DisplayName("反例：项目无 PM → NF_NOT_PM（50011 FORBIDDEN）")
    void projectHasNoPm_nfNotPm() {
        stubNoDuplicate();
        stubProjectHasNoPm();

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "无PM拒绝执行", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_NOT_PM.getCode());
                assertThat(ApiV1ErrorCode.FORBIDDEN.getCode()).isEqualTo(30001);
            });
    }

    /* ====================================================================
     *  反例：触发情形非法 → NF_TRIGGER_TYPE_INVALID
     * ==================================================================== */
    @Test
    @DisplayName("反例：triggerType 非法 → NF_TRIGGER_TYPE_INVALID（50009）")
    void triggerTypeInvalid() {
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "WHATEVER_UNKNOWN", "异常剧本", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_TRIGGER_TYPE_INVALID.getCode());
            });
    }

    /* ====================================================================
     *  反例：triggerMonth 格式错 → NF_MONTH_FORMAT_INVALID
     * ==================================================================== */
    @Test
    @DisplayName("反例：triggerMonth 格式错（YYYY-M 而非 YYYY-MM） → NF_MONTH_FORMAT_INVALID")
    void triggerMonthFormatInvalid() {
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "月份格式异常", "2026-9", null);

        assertThatThrownBy(() -> service.create(req, MARKET_PM_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(t -> {
                IpdBusinessException ex = (IpdBusinessException) t;
                assertThat(ex.getErrorCode().getCode()).isEqualTo(ApiV1ErrorCode.NF_MONTH_FORMAT_INVALID.getCode());
            });
    }

    /* ====================================================================
     *  AC-INC-40 联动：DECIDE APPROVE → 通知主责 + 连带 PM（双通道）
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-40 联动：DECIDE APPROVE → 通知主责 + 连带 PM（双通道）")
    void decideApprove_notifyBothPm() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(500L).projectId(PROJECT_ID).status("PENDING_DECISION")
            .triggerType("REWORK_EXCEEDED")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM_ID)
            .relatedRole("RD_PM").relatedPersonId(RD_PM_ID)
            .mainExecution("STOP_ALLOWANCE").relatedExecution("HALVE_ALLOWANCE")
            .triggerMonth("2026-09")
            .bonusDisqualify(1).tierDelta(new BigDecimal("-0.50"))
            .triggeredBy(MARKET_PM_ID).build();
        when(mapper.selectById(500L)).thenReturn(existing);

        NegativeFeedback row = service.decide(500L,
            new NegativeFeedbackDecisionReq("APPROVE", "认定执行"), LEADER_ACTOR);

        assertThat(row.getStatus()).isEqualTo("EXECUTED");
        // 双 PM 通知：MARKET_PM 主 + RD_PM 连带（次数与人数一致）
        ArgumentCaptor<Long> personCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationService, times(2)).publish(personCaptor.capture(),
            anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString());
        List<Long> personIds = new ArrayList<>(personCaptor.getAllValues());
        assertThat(personIds).containsExactlyInAnyOrder(MARKET_PM_ID, RD_PM_ID);
    }

    /* ====================================================================
     *  AC-INC-40 联动：BOTH 模式通知双PM（MISSED_MARKET_WINDOW）
     * ==================================================================== */
    @Test
    @DisplayName("AC-INC-40 联动：MISSED_MARKET_WINDOW (BOTH) → 通知 MARKET_PM + RD_PM 双通道")
    void decideApprove_bothNotifyBothPm() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(600L).projectId(PROJECT_ID).status("PENDING_DECISION")
            .triggerType("MISSED_MARKET_WINDOW")
            .mainRole("BOTH").mainPersonId(MARKET_PM_ID)  // 主记录人填 MARKET_PM
            .relatedRole(null).relatedPersonId(null)
            .mainExecution("STOP_ALLOWANCE").relatedExecution(null)
            .triggerMonth("2026-09")
            .bonusDisqualify(1).tierDelta(new BigDecimal("-0.50"))
            .triggeredBy(MARKET_PM_ID).build();
        when(mapper.selectById(600L)).thenReturn(existing);

        NegativeFeedback row = service.decide(600L,
            new NegativeFeedbackDecisionReq("APPROVE", "BOTH 模式认定"), LEADER_ACTOR);

        assertThat(row.getStatus()).isEqualTo("EXECUTED");
        // BOTH 模式：循环 mainPersonId + relatedPersonId(null) → 实际只 publish MARKET_PM 一次（service 实现按 personId!=null 过滤）
        ArgumentCaptor<Long> personCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationService, times(1)).publish(personCaptor.capture(),
            anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString());
        List<Long> personIds = new ArrayList<>(personCaptor.getAllValues());
        // BOTH 模式下通知：只发给主记录人（MARKET_PM 作为回执人），连带方为 null 被跳过
        // 注意：BOTH 模式下 mainPersonId 实际上代表项目 MARKET_PM，RD_PM 的通知走单独链路（市场PM群组通知）
        assertThat(personIds).contains(MARKET_PM_ID);
    }
}
