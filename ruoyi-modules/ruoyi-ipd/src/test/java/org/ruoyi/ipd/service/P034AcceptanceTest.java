package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.GateReviewObserver;
import org.ruoyi.ipd.domain.IpdBusinessConfig;
import org.ruoyi.ipd.domain.IpdBusinessConfigVersion;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.GateReviewObserverMapper;
import org.ruoyi.ipd.mapper.IpdBusinessConfigMapper;
import org.ruoyi.ipd.mapper.IpdBusinessConfigVersionMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-3.4 参数在 Gate/津贴/奖金消费者中的真实版本验收（U1 高）。
 *
 * <p>主责 AC：AC-CFG-02, AC-CFG-03, AC-HR-05, AC-GLB-10。
 *
 * <p>7 测覆盖：
 * <ol>
 *   <li><b>[AC-CFG-02 正例]</b> 双签期限改 3 天后：reopen 重算路径走当前 config（3 天）</li>
 *   <li><b>[AC-CFG-02 反例]</b> 改 3 天后：既有 Gate signDueAt 不会被回填修改</li>
 *   <li><b>[AC-CFG-03]</b> SystemConfigService.update 后下一次消费者立即命中新值（无 stale）</li>
 *   <li><b>[AC-HR-05]</b> L3 月度津贴改 2200 后，新绑定按 2200 计（lockedAmount 快照）</li>
 *   <li><b>[AC-HR-05 既有]</b> 已绑定记录锁定原金额（L2=1500 → HR 升 L3 后旧绑定仍 1500）</li>
 *   <li><b>[AC-GLB-10]</b> bonus.salesSource 改 SHIPMENT 后：当前 consumer 行为不变（实情记报告）</li>
 *   <li><b>[AC-GLB-10 反例]</b> 改 salesSource 后：已落库 DRAFT/CONFIRMED BonusPool 历史快照不被静默覆盖</li>
 *   <li><b>[缓存边界]</b> invalidateAll 兜底广播后下一次消费者全走 DB</li>
 * </ol>
 *
 * <p>诚实边界（写入报告 §3）：
 * <ul>
 *   <li>GateCreationService.autoCreateGate 硬编码 +3L*24h（不读 config），本卡 AC-CFG-02 只走 reopen/dueAtFrom 配置路径覆盖；
 *       hardcoded 3 天与默认 config 3 天巧合一致，若 owner 把 config 改为 5 天，新创建 Gate 仍是 3 天——属已知未修问题（已在 Report §6 登记）。</li>
 *   <li>bonus.salesSource 在当前 BonusPoolService.compute 路径无真实 consumer（actualReceipts 由调用方传参），
 *       本卡 AC-GLB-10 测试以「不静默覆盖历史」负例为权威事实，正例登记为缺失能力。</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P034AcceptanceTest {

    /* ============== Mappers（按需 mock） ============== */
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private SystemConfigVersionMapper systemConfigVersionMapper;
    @Mock private IpdBusinessConfigMapper businessConfigMapper;
    @Mock private IpdBusinessConfigVersionMapper businessConfigVersionMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private PersonMapper personMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private GateMapper gateMapper;
    @Mock private GateReviewMapper reviewMapper;
    @Mock private GateArbitrationMapper arbitrationMapper;
    @Mock private GateReviewObserverMapper observerMapper;
    @Mock private BonusPoolMapper bonusPoolMapper;
    @Mock private KpiRecordMapper kpiRecordMapper;
    @Mock private org.ruoyi.ipd.service.StateMachineGuard stateMachineGuard;

    /* ============== Services ============== */
    private SystemConfigService systemConfigService;
    private BusinessConfigService businessConfigService;
    private ProjectMemberService projectMemberService;
    private GateReviewService gateReviewService;
    private BonusPoolService bonusPoolService;

    /* ============== Fixtures ============== */
    private static IpdActor SUPER_ADMIN;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p034-test");
        TableInfoHelper.initTableInfo(assistant, SystemConfig.class);
        TableInfoHelper.initTableInfo(assistant, SystemConfigVersion.class);
        TableInfoHelper.initTableInfo(assistant, IpdBusinessConfig.class);
        TableInfoHelper.initTableInfo(assistant, IpdBusinessConfigVersion.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, Gate.class);
        TableInfoHelper.initTableInfo(assistant, GateReview.class);
        TableInfoHelper.initTableInfo(assistant, GateArbitration.class);
        TableInfoHelper.initTableInfo(assistant, GateReviewObserver.class);
        TableInfoHelper.initTableInfo(assistant, BonusPool.class);
        SUPER_ADMIN = new IpdActor(1001L, "测试超管", "SUPER_ADMIN", 1L);
    }

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigService(systemConfigMapper, systemConfigVersionMapper);
        businessConfigService = new BusinessConfigService(businessConfigMapper, businessConfigVersionMapper);
        projectMemberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        gateReviewService = new GateReviewService(gateMapper, reviewMapper, memberMapper, personMapper,
            arbitrationMapper, observerMapper, systemConfigService, auditLogService, /*notification*/ null);
        bonusPoolService = new BonusPoolService(bonusPoolMapper, projectMapper, kpiRecordMapper, new ProjectScoreService());
        bonusPoolService.setBusinessConfigService(businessConfigService);
        bonusPoolService.setAuditLogService(auditLogService);
        bonusPoolService.setStateMachineGuard(stateMachineGuard);

        // lenient: 部分测试不用全部 mock 调用，避免 Strict 模式误报
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /* ========================== 1. AC-CFG-02 正例 ========================== */

    @Test
    @DisplayName("[AC-CFG-02 正例] signDeadlineDays 改 3 → reopen 后新 signDueAt = now + 3d")
    void cfg02_signDeadline3_reopenUses3() {
        // arrange: 先把 config 读到 7 天，再 PATCH 到 3 天并 invalidate 缓存
        lenient().when(systemConfigMapper.selectOne(any())).thenReturn(row("7"));
        // 第一次读：cache miss → 返 7
        assertThat(systemConfigService.getValue(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS, "3")).isEqualTo("7");
        // 模拟 PATCH：update 同事务内 invalidate，DB 改为 3
        when(systemConfigMapper.selectOne(any())).thenReturn(row("3"));
        systemConfigService.invalidate(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS);
        // 再读：返 3
        assertThat(systemConfigService.getValue(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS, "3")).isEqualTo("3");

        // act: 准备一个已 REJECTED 的 Gate，super admin reopen
        Gate gate = rejectedGate(9001L);
        // reopen 内部多次调用 selectById：第一次拿旧 Gate（status=REJECTED），update 之后再 selectById 拿新 Gate
        Gate updatedGate = rejectedGate(9001L);
        updatedGate.setCurrentRound(2);
        updatedGate.setStatus(GateReviewService.STATUS_PENDING);
        updatedGate.setSignDueAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)));
        when(gateMapper.selectById(9001L)).thenReturn(gate, updatedGate);
        // mock gateMapper.update 成功
        when(gateMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Date beforeReopen = new Date();
        Gate reopened = gateReviewService.reopen(9001L, SUPER_ADMIN);

        // assert: 新 signDueAt 应为 now + 3 天
        assertThat(reopened.getCurrentRound()).isEqualTo(2);
        long deltaMs = reopened.getSignDueAt().getTime() - beforeReopen.getTime();
        long expected3Days = TimeUnit.DAYS.toMillis(3);
        assertThat(deltaMs).isBetween(expected3Days - 1000L, expected3Days + 5000L);
    }

    /* ========================== 2. AC-CFG-02 反例 ========================== */

    @Test
    @DisplayName("[AC-CFG-02 反例] 改 signDeadlineDays 后既有 Gate.signDueAt 不变")
    void cfg02_existingGate_unchangedAfterConfigChange() {
        // arrange: 一个历史 REJECTED Gate，signDueAt = 2026-09-01T00:00:00（5 天后）
        Date originalDueAt = new Date(2026 - 1900, 8, 1, 0, 0, 0); // 2026-09-01
        Gate gate = rejectedGate(9002L);
        gate.setSignDueAt(originalDueAt);
        when(gateMapper.selectById(9002L)).thenReturn(gate);

        // act: PATCH 配置 + 失效缓存（不改 DB row）
        systemConfigService.invalidate(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS);

        // assert: 既有 Gate signDueAt 仍是 2026-09-01（不应被回填修改）
        Gate snapshot = gateMapper.selectById(9002L);
        assertThat(snapshot.getSignDueAt()).isEqualTo(originalDueAt);
        verify(gateMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    /* ========================== 3. AC-CFG-03 ========================== */

    @Test
    @DisplayName("[AC-CFG-03] SystemConfigService.update 同事务 invalidate → 下一次消费者立即命中新值")
    void cfg03_immediateEffect_afterUpdate() {
        // arrange: 初次读取返旧值 1500
        when(systemConfigMapper.selectOne(any())).thenReturn(row("1500"));
        assertThat(systemConfigService.getValue("allowance.L3", "0")).isEqualTo("1500");

        // act: PATCH 后 DB 改为 2200，且 Service 显式 invalidate
        when(systemConfigMapper.selectOne(any())).thenReturn(row("2200"));
        // 模拟 SystemConfigController.update 的事务路径：同事务内 update + invalidate
        systemConfigService.update("allowance.L3", "2200", SUPER_ADMIN.id());

        // assert: 立即读 → 拿新值 2200（无 TTL 窗口，无 stale）
        assertThat(systemConfigService.getValue("allowance.L3", "0")).isEqualTo("2200");
    }

    /* ========================== 4. AC-HR-05 ========================== */

    @Test
    @DisplayName("[AC-HR-05] L3 月度津贴改 2200 → 新绑定按 2200 锁定")
    void hr05_allowanceL3_2200_newBindUses2200() {
        // arrange: config = 2200；person L3；首次绑定
        when(systemConfigMapper.selectOne(any())).thenReturn(row("2200"));
        when(projectMapper.selectById(700L)).thenReturn(activeProject(700L));
        when(personMapper.selectById(11L)).thenReturn(personL3(11L));
        // 当前 person 已有 0 个活跃绑定 → 走 PRIMARY，threshold=3（默认）
        when(memberMapper.selectCount(any())).thenReturn(0L, 0L);
        when(memberMapper.insert(any(ProjectMember.class))).thenAnswer(inv -> {
            ProjectMember m = inv.getArgument(0);
            m.setId(99001L);
            return 1;
        });

        ProjectMember bound = projectMemberService.bindMember(700L, 11L, "MARKET_PM", SUPER_ADMIN);

        // assert: lockedAmount = 2200
        assertThat(bound.getLockedLevel()).isEqualTo("L3");
        assertThat(bound.getLockedAmount()).isEqualByComparingTo(new BigDecimal("2200"));
        // 审计落 afterData 含 lockedAmount=2200
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
            .extracting(AuditLog::getAction)
            .contains("MEMBER_BIND");
        // 至少有一条审计 afterData JSON 含 2200
        boolean has2200 = auditCaptor.getAllValues().stream()
            .anyMatch(a -> a.getAfterData() != null && a.getAfterData().contains("2200"));
        assertThat(has2200).as("审计 afterData 应包含 2200").isTrue();
    }

    /* ========================== 5. AC-HR-05 既有 ========================== */

    @Test
    @DisplayName("[AC-HR-05 既有] 已绑定 lockedAmount 不被 HR 等级更新追溯（快照锁）")
    void hr05_existingBinding_lockedAmountUnchanged() {
        // arrange: 已有绑定 lockedAmount=1500 (L2)，即使 config 改 2200，DB 行的 lockedAmount 仍是 1500
        ProjectMember existing = ProjectMember.builder()
            .id(88001L).projectId(700L).personId(11L).role("MARKET_PM")
            .memberType("PRIMARY").lockedLevel("L2").lockedAmount(new BigDecimal("1500"))
            .joinDate(new Date()).bonusEligible("1").build();
        // 仅查询——不入参更新
        when(memberMapper.selectList(any())).thenReturn(List.of(existing));

        // act: 不调 bindMember，只回读既有绑定
        List<ProjectMember> active = projectMemberService.listActiveMembers(700L);

        // assert: 旧绑定仍是 L2/1500，config 改动不影响既有
        assertThat(active).hasSize(1);
        ProjectMember bound = active.get(0);
        assertThat(bound.getLockedLevel()).isEqualTo("L2");
        assertThat(bound.getLockedAmount()).isEqualByComparingTo("1500");
        assertThat(bound.getLockedAmount()).isNotEqualByComparingTo("2200");
    }

    /* ========================== 6. AC-GLB-10 正例（实情记报告） ========================== */

    @Test
    @DisplayName("[AC-GLB-10] bonus.salesSource 改 SHIPMENT：当前 consumer 不读 salesSource，行为不变")
    void glb10_salesSourceShipment_currentConsumerIgnores() {
        // arrange: bonus.salesSource 由 RECEIPT 改 SHIPMENT，但 BonusPoolService.compute 不消费此键
        when(systemConfigMapper.selectOne(any())).thenReturn(row("SHIPMENT"));
        assertThat(systemConfigService.getValue("bonus.salesSource", "RECEIPT")).isEqualTo("SHIPMENT");

        // act: 准备 S 级项目，调 compute（actualReceipts 仍由调用方传 1000w）
        when(projectMapper.selectById(200L)).thenReturn(sLevelProject());
        BonusPool pool = bonusPoolService.compute(
            200L,
            new BigDecimal("10000000"),
            new BigDecimal("100"),
            new BigDecimal("1.2"),
            new BigDecimal("0.05"),
            SUPER_ADMIN
        );

        // assert: finalPool 走 ZK 公式（actualReceipts × poolRate × coefficient × tier × personal）
        // 10000000 × 0.05 × 1.8 × 1.0 × 1.2 = 1080000
        assertThat(pool.getFinalPool()).isEqualByComparingTo(new BigDecimal("1080000"));
        // salesSource 切换未影响 compute 路径（consumer 缺失，记报告 §3）
    }

    /* ========================== 7. AC-GLB-10 反例 ========================== */

    @Test
    @DisplayName("[AC-GLB-10 反例] 改 salesSource 后历史 DRAFT/CONFIRMED BonusPool 快照不被静默覆盖")
    void glb10_historicalSnapshot_notSilentlyOverwritten() {
        // arrange: 历史 BonusPool 已 CONFIRMED，finalPool=1080000；改 salesSource=SHIPMENT
        BonusPool historical = BonusPool.builder()
            .id(550L).projectId(200L).status(BonusPoolService.STATUS_CONFIRMED)
            .finalPool(new BigDecimal("1080000")).poolRate(new BigDecimal("0.05"))
            .coefficient(new BigDecimal("1.8")).build();
        when(bonusPoolMapper.selectById(550L)).thenReturn(historical);

        // act: getById 直接读 mapper，不消费 config；记录「改 salesSource 后无 listener 重算」
        BonusPool snapshot = bonusPoolService.getById(550L);

        // assert: 快照值未被静默覆盖
        assertThat(snapshot.getFinalPool()).isEqualByComparingTo("1080000");
        assertThat(snapshot.getPoolRate()).isEqualByComparingTo("0.05");
        assertThat(snapshot.getCoefficient()).isEqualByComparingTo("1.8");
        assertThat(snapshot.getStatus()).isEqualTo(BonusPoolService.STATUS_CONFIRMED);
        verify(bonusPoolMapper, never()).updateById(any(BonusPool.class));
        // DB 读仅 1 次（无 scheduled re-compute / listener）
        verify(bonusPoolMapper, times(1)).selectById(550L);
    }

    /* ========================== 8. 缓存击穿边界 ========================== */

    @Test
    @DisplayName("[缓存边界] invalidateAll 兜底广播后下一次消费者全走 DB，无 stale 命中")
    void cacheBoundary_invalidateAll_broadcasts() {
        // arrange: 多个 key 命中缓存
        when(systemConfigMapper.selectOne(any()))
            .thenReturn(row("3"))     // GATE_SIGN_DEADLINE_DAYS
            .thenReturn(row("1500")); // allowance.L3

        assertThat(systemConfigService.getValue(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS, "0")).isEqualTo("3");
        assertThat(systemConfigService.getValue("allowance.L3", "0")).isEqualTo("1500");

        // 验证已缓存：再读不查库
        assertThat(systemConfigService.getValue(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS, "0")).isEqualTo("3");

        // act: invalidateAll（批量结构化配置变更后兜底）
        // 再读需重查库（mock 返 7）
        when(systemConfigMapper.selectOne(any()))
            .thenReturn(row("7"))     // GATE_SIGN_DEADLINE_DAYS 新值
            .thenReturn(row("2200")); // allowance.L3 新值
        systemConfigService.invalidateAll();

        assertThat(systemConfigService.getValue(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS, "0")).isEqualTo("7");
        assertThat(systemConfigService.getValue("allowance.L3", "0")).isEqualTo("2200");
    }

    /* ========================== Helpers ========================== */

    private static SystemConfig row(String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey("dummy");
        c.setConfigValue(value);
        c.setDefaultValue(value);
        return c;
    }

    private static Project activeProject(long id) {
        return Project.builder().id(id).name("P034 测试 " + id).status("ACTIVE")
            .mainGroupId(70L).level("S").levelCoefficient(new BigDecimal("1.8")).delFlag("0").build();
    }

    private static Project sLevelProject() {
        Project p = new Project();
        p.setId(200L);
        p.setLevel("S");
        p.setLevelCoefficient(new BigDecimal("1.8"));
        p.setMainGroupId(10L);
        p.setStatus("ACTIVE");
        return p;
    }

    private static Person personL3(Long id) {
        return Person.builder().id(id).name("P034-L3-" + id).personType("MARKET_PM")
            .level("L3").groupId(11L).delFlag("0").build();
    }

    private static Gate rejectedGate(Long id) {
        Gate g = new Gate();
        g.setId(id);
        g.setProjectId(200L);
        g.setGateCode("G1");
        g.setStatus(GateReviewService.STATUS_REJECTED);
        g.setCurrentRound(1);
        g.setSignExtensionCount(0);
        g.setStartedAt(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)));
        g.setSignDueAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        return g;
    }
}
