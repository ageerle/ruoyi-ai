package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.service.KpiRecordService.KpiSourceItem;
import org.ruoyi.ipd.service.KpiRecordService.TrendPoint;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-1.1 / P3-1.2 / P3-1.3 KPI 三张卡单测（合测）
 *
 * <p>覆盖 6 维度（每张卡 2 测例 + 跨卡 2 测例）：
 * <ol>
 *   <li>正常路径：3 数据源齐备 / 5+1 桶齐备 / 12 月趋势齐备</li>
 *   <li>边界：缺数据 → 空 / 全 0 / 补 MISSING</li>
 *   <li>异常：period 错 / periods 越界</li>
 *   <li>权限：cross-person 不允许（actor 隔离校验，service 层不抛，依赖 controller 层）</li>
 *   <li>审计：仅查询，不写库</li>
 *   <li>幂等：多次调用结果一致</li>
 * </ol>
 *
 * <p>AC：AC-KPI-01~05/15/16b/17~22；BR：BR-KPI-01/02/03/06/08。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KpiRecordServiceTest {

    @Mock
    private KpiRecordMapper kpiRecordMapper;
    @Mock
    private ProjectScoreMapper projectScoreMapper;
    @Mock
    private AllowanceLedgerMapper allowanceLedgerMapper;
    @Mock
    private BonusPoolMapper bonusPoolMapper;
    /** ROOT-R3-P0-2：跨状态机守卫 mock（Wave17 KPI 状态机接入） */
    @Mock
    private StateMachineGuard stateMachineGuard;

    private KpiRecordService service;

    private static final Long ACTOR_ID = 1001L;
    private static final String PERIOD = "2026-08";

    private final IpdActor actor = new IpdActor(ACTOR_ID, "TestPM", "MARKET_PM", 1L);

    @BeforeEach
    void setUp() {
        service = new KpiRecordService(kpiRecordMapper, projectScoreMapper,
            allowanceLedgerMapper, bonusPoolMapper);
        // ROOT-R3-P0-2：注入 mock 守卫（Wave17 KPI 状态机测试前置条件）
        service.setStateMachineGuard(stateMachineGuard);
    }

    // ============================================================
    //  P3-1.1 功能 KPI 指标来源
    // ============================================================

    @Test
    @DisplayName("P3-1.1 正例：3 数据源齐备 → 3 个 KpiSourceItem，weight 之和 = 1.0")
    void calculateFunctionalKpi_threeSourcesAggregated() {
        // 准备 3 数据源
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(projectScoreOf("85.00"));
        when(allowanceLedgerMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceOf("3000.00", "L3"));

        List<KpiSourceItem> items = service.calculateFunctionalKpi(actor, PERIOD);

        assertThat(items).hasSize(3);
        assertThat(items).extracting(KpiSourceItem::source)
            .containsExactlyInAnyOrder(
                KpiRecordService.SRC_PROJECT_SCORE,
                KpiRecordService.SRC_KPI_CALCULATOR,
                KpiRecordService.SRC_ALLOWANCE_LEDGER);
        // weight 之和 = 1.0
        BigDecimal totalWeight = items.stream()
            .map(KpiSourceItem::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo("1.00");
        // contribution 保留 2 位
        items.forEach(i ->
            assertThat(i.contribution().scale()).isEqualTo(2));
    }

    @Test
    @DisplayName("P3-1.1 异常：period 格式错 → IpdBusinessException")
    void calculateFunctionalKpi_invalidPeriod() {
        assertThatThrownBy(() -> service.calculateFunctionalKpi(actor, "2026-9"))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.calculateFunctionalKpi(actor, "abc"))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.calculateFunctionalKpi(actor, ""))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.calculateFunctionalKpi(actor, null))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("P3-1.1 边界：projectScore 缺数 → 仅返回 2 个 source")
    void calculateFunctionalKpi_missingProjectScore() {
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);
        when(allowanceLedgerMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceOf("2000.00", "L2"));

        List<KpiSourceItem> items = service.calculateFunctionalKpi(actor, PERIOD);

        // KPI_CALCULATOR 是纯函数必返；ALLOWANCE 返；PROJECT_SCORE 缺数跳过
        assertThat(items).hasSize(2);
        assertThat(items).extracting(KpiSourceItem::source)
            .doesNotContain(KpiRecordService.SRC_PROJECT_SCORE);
    }

    // ============================================================
    //  P3-1.2 绩效 KPI 聚合
    // ============================================================

    @Test
    @DisplayName("P3-1.2 正例：6 桶齐备（L1~L5 + COMPREHENSIVE）")
    void aggregatePerformanceKpi_allSixLevels() {
        // L1~L5 各返一条
        when(allowanceLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceListForLevels());
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(projectScoreOf("85.00"));

        Map<String, BigDecimal> result = service.aggregatePerformanceKpi(actor, PERIOD);

        assertThat(result).containsOnlyKeys("L1", "L2", "L3", "L4", "L5", "COMPREHENSIVE");
        assertThat(result.get("L3")).isEqualByComparingTo("3000.00");
        // COMPREHENSIVE：85 → tier 0.8 → ×100 = 80.00
        assertThat(result.get("COMPREHENSIVE")).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("P3-1.2 边界：actor 当期无任何记录 → 6 桶全为 0")
    void aggregatePerformanceKpi_emptyAllZero() {
        when(allowanceLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);

        Map<String, BigDecimal> result = service.aggregatePerformanceKpi(actor, PERIOD);

        assertThat(result).hasSize(6);
        result.values().forEach(v -> assertThat(v).isEqualByComparingTo("0"));
    }

    // ============================================================
    //  P3-1.3 历史 KPI 趋势
    // ============================================================

    @Test
    @DisplayName("P3-1.3 正例：12 期回看，6 条命中 6 条 MISSING")
    void getHistoricalTrend_twelvePeriods() {
        // 当前 8 月，回看 12 期 → 2025-09 ~ 2026-08
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(11);
        List<KpiRecord> records = new ArrayList<>();
        // 每隔 1 个月 1 条，6 条命中
        for (int i = 0; i < 6; i++) {
            KpiRecord r = new KpiRecord();
            r.setId((long) (i + 1));
            r.setPersonId(ACTOR_ID);
            r.setPeriod(startMonth.plusMonths(i * 2).toString());
            r.setComprehensiveScore(new BigDecimal("75.00").add(new BigDecimal(i)));
            records.add(r);
        }
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);

        List<TrendPoint> points = service.getHistoricalTrend(actor, 12);

        assertThat(points).hasSize(12);
        long dataCount = points.stream().filter(p -> "DATA".equals(p.source())).count();
        long missingCount = points.stream().filter(p -> "MISSING".equals(p.source())).count();
        assertThat(dataCount).isEqualTo(6);
        assertThat(missingCount).isEqualTo(6);
        // 顺序：升序
        for (int i = 1; i < points.size(); i++) {
            assertThat(points.get(i).period()).isGreaterThan(points.get(i - 1).period());
        }
    }

    @Test
    @DisplayName("P3-1.3 异常：periods 越界（0 或 100）→ IpdBusinessException")
    void getHistoricalTrend_invalidPeriods() {
        assertThatThrownBy(() -> service.getHistoricalTrend(actor, 0))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.getHistoricalTrend(actor, -1))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.getHistoricalTrend(actor, 37))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.getHistoricalTrend(actor, 100))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("P3-1.3 边界：actor 无任何记录 → 12 个 MISSING 点")
    void getHistoricalTrend_emptyAllMissing() {
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        List<TrendPoint> points = service.getHistoricalTrend(actor, 12);

        assertThat(points).hasSize(12);
        assertThat(points).allMatch(p -> "MISSING".equals(p.source()));
        assertThat(points).allMatch(p -> p.value().compareTo(BigDecimal.ZERO) == 0);
    }

    // ============================================================
    //  跨卡：幂等 + 兼容旧签名
    // ============================================================

    @Test
    @DisplayName("幂等：calculateFunctionalKpi 两次调用结果一致")
    void calculateFunctionalKpi_idempotent() {
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(projectScoreOf("85.00"));
        when(allowanceLedgerMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceOf("3000.00", "L3"));

        List<KpiSourceItem> first = service.calculateFunctionalKpi(actor, PERIOD);
        List<KpiSourceItem> second = service.calculateFunctionalKpi(actor, PERIOD);

        assertThat(first).hasSize(second.size());
        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).source()).isEqualTo(second.get(i).source());
            assertThat(first.get(i).value()).isEqualByComparingTo(second.get(i).value());
            assertThat(first.get(i).weight()).isEqualByComparingTo(second.get(i).weight());
        }
    }

    @Test
    @DisplayName("兼容：旧签名（KpiRecordService 字段校验）仍可独立调用")
    void oldSignaturesStillWork() {
        // AC-KPI-01
        assertThat(service.saveKpiWeights(new BigDecimal("0.60")))
            .isEqualByComparingTo("0.60");
        // AC-KPI-04
        assertThat(service.computeComprehensive(new BigDecimal("80"), new BigDecimal("70"), new BigDecimal("0.60")))
            .isEqualByComparingTo("76.00");
    }

    // ============================================================
    //  helpers
    // ============================================================

    private ProjectScore projectScoreOf(String weighted) {
        ProjectScore ps = new ProjectScore();
        ps.setId(1L);
        ps.setPersonId(ACTOR_ID);
        ps.setWeightedScore(new BigDecimal(weighted));
        ps.setStatus("FINALIZED");
        return ps;
    }

    private AllowanceLedger allowanceOf(String finalAmount, String level) {
        AllowanceLedger l = new AllowanceLedger();
        l.setId(1L);
        l.setPersonId(ACTOR_ID);
        l.setMonth(PERIOD);
        l.setLockedLevel(level);
        l.setFinalAmount(new BigDecimal(finalAmount));
        return l;
    }

    private List<AllowanceLedger> allowanceListForLevels() {
        return new ArrayList<>(Arrays.asList(
            allowanceOf("1000.00", "L1"),
            allowanceOf("2000.00", "L2"),
            allowanceOf("3000.00", "L3"),
            allowanceOf("4000.00", "L4"),
            allowanceOf("5000.00", "L5")
        ));
    }

    // ============================================================
    //  ROOT-R1 P0-7 字面量迁移：BusinessConfigService 验证
    // ============================================================

    @Test
    @DisplayName("ROOT-R1 P0-7: 注入 BusinessConfigService → 聚合 KPI 用 config 阈值 75（覆盖硬编码 60）")
    void businessConfigService_overridesHardcodedKpiDefault() {
        // 给 5 档津贴补数：避免聚合路径因缺数短路
        when(allowanceLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceListForLevels());
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(projectScoreOf("85.00"));

        // 注入 BusinessConfigService（mock）返回 75 覆盖硬编码 60
        org.ruoyi.ipd.service.BusinessConfigService businessConfigService =
            org.mockito.Mockito.mock(org.ruoyi.ipd.service.BusinessConfigService.class);
        when(businessConfigService.getBigDecimal(org.ruoyi.ipd.common.BusinessConfigKeys.KPI_STOP_THRESHOLD))
            .thenReturn(new BigDecimal("75"));

        KpiRecordService serviceWithConfig = new KpiRecordService(
            kpiRecordMapper, projectScoreMapper, allowanceLedgerMapper, bonusPoolMapper, businessConfigService);

        // 触发 _queryCalculatorValue 的间接路径：calculateFunctionalKpi
        List<KpiSourceItem> items = serviceWithConfig.calculateFunctionalKpi(actor, PERIOD);
        // 3 数据源聚合中 SRC_KPI_CALCULATOR 项的 value 应来自 BusinessConfigService (=75)
        KpiSourceItem calcItem = items.stream()
            .filter(i -> KpiRecordService.SRC_KPI_CALCULATOR.equals(i.source()))
            .findFirst().orElseThrow();
        assertThat(calcItem.value()).isEqualByComparingTo("75");
    }

    @Test
    @DisplayName("ROOT-R1 P0-7: BusinessConfigService 未注入 → 回退硬编码 60（兼容旧测试）")
    void businessConfigService_nullFallsBackToHardcoded() {
        when(allowanceLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(allowanceListForLevels());
        when(projectScoreMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(projectScoreOf("85.00"));

        // service 用 4 依赖构造器（businessConfigService = null）
        List<KpiSourceItem> items = service.calculateFunctionalKpi(actor, PERIOD);
        KpiSourceItem calcItem = items.stream()
            .filter(i -> KpiRecordService.SRC_KPI_CALCULATOR.equals(i.source()))
            .findFirst().orElseThrow();
        assertThat(calcItem.value()).isEqualByComparingTo("60");
    }

    // ============================================================
    //  Wave17 ROOT-R3-P0-2：KPI 状态机接入 StateMachineGuard（3 测）
    // ============================================================

    /**
     * Wave17-1（合法迁移）：approveKpi EDITING→APPROVED 合法迁移，
     * 守卫 preCheck 通过 + updateById 触发 + postCommit 注册（非事务上下文直接执行）。
     */
    @Test
    @DisplayName("Wave17: approveKpi 合法迁移 EDITING→APPROVED → guard.preCheck + updateById + postCommit 联动")
    void approveKpi_legalTransition_callsGuardAndUpdates() {
        Long recordId = 42L;
        KpiRecord record = new KpiRecord();
        record.setId(recordId);
        record.setStatus(KpiRecordService.ST_EDITING);
        when(kpiRecordMapper.selectById(recordId)).thenReturn(record);

        KpiRecord result = service.approveKpi(recordId, actor);

        assertThat(result.getStatus()).isEqualTo(KpiRecordService.ST_APPROVED);
        verify(stateMachineGuard).preCheck(
            KpiRecordService.KPI_RECORD_ENTITY_TYPE,
            KpiRecordService.ST_EDITING,
            KpiRecordService.ST_APPROVED,
            "approve");
        verify(kpiRecordMapper).updateById(record);
        verify(stateMachineGuard).postCommit(
            eq(KpiRecordService.KPI_RECORD_ENTITY_TYPE),
            eq(KpiRecordService.ST_EDITING),
            eq(KpiRecordService.ST_APPROVED),
            eq("approve"),
            eq(ACTOR_ID),
            eq(recordId),
            any(Date.class));
    }

    /**
     * Wave17-2（非法迁移）：guard 抛 IpdBusinessException 时服务透传异常，且 updateById 不触发。
     */
    @Test
    @DisplayName("Wave17: approveKpi 非法迁移 guard 抛 → IpdBusinessException 透传 + updateById 不触发")
    void approveKpi_illegalTransition_throwsIpdBusinessException() {
        Long recordId = 43L;
        KpiRecord record = new KpiRecord();
        record.setId(recordId);
        record.setStatus(KpiRecordService.ST_LOCKED); // LOCKED→APPROVED 非法
        when(kpiRecordMapper.selectById(recordId)).thenReturn(record);
        doThrow(new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "非法迁移 LOCKED→APPROVED"))
            .when(stateMachineGuard).preCheck(
                KpiRecordService.KPI_RECORD_ENTITY_TYPE,
                KpiRecordService.ST_LOCKED,
                KpiRecordService.ST_APPROVED,
                "approve");

        assertThatThrownBy(() -> service.approveKpi(recordId, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("非法迁移");

        verify(stateMachineGuard).preCheck(
            KpiRecordService.KPI_RECORD_ENTITY_TYPE,
            KpiRecordService.ST_LOCKED,
            KpiRecordService.ST_APPROVED,
            "approve");
        verify(kpiRecordMapper, never()).updateById(any(KpiRecord.class));
        // 守卫抛异常时 postCommit 不应触发（事务未提交语义）
        verify(stateMachineGuard, never()).postCommit(
            any(), any(), any(), any(), any(), any(), any(Date.class));
    }

    /**
     * Wave17-3（postCommit 触发）：recordScore 初始置位 EDITING，null→EDITING 合法迁移；
     * insert 后 registerPostCommit 调用（无事务上下文 → 直接执行 guard.postCommit）。
     */
    @Test
    @DisplayName("Wave17: recordScore 初始置位 EDITING → guard.preCheck(null, EDITING) + insert + postCommit 触发")
    void recordScore_initialState_callsPostCommit() {
        KpiRecord draft = new KpiRecord();
        draft.setPersonId(ACTOR_ID);
        draft.setPeriod(PERIOD);
        draft.setComprehensiveScore(new BigDecimal("75.00"));
        // 模拟 insert 自动生成主键
        when(kpiRecordMapper.insert(any(KpiRecord.class))).thenAnswer(inv -> {
            KpiRecord r = inv.getArgument(0);
            r.setId(99L);
            return 1;
        });

        KpiRecord saved = service.recordScore(draft, actor);

        assertThat(saved.getStatus()).isEqualTo(KpiRecordService.ST_EDITING);
        assertThat(saved.getId()).isEqualTo(99L);
        // preCheck：from=null（未持久化草稿）→ to=EDITING
        verify(stateMachineGuard).preCheck(
            eq(KpiRecordService.KPI_RECORD_ENTITY_TYPE),
            isNull(),
            eq(KpiRecordService.ST_EDITING),
            eq("record"));
        // insert 触发
        verify(kpiRecordMapper).insert(draft);
        // postCommit：实体 ID 99L（insert 后） + operatorId=ACTOR_ID
        verify(stateMachineGuard).postCommit(
            eq(KpiRecordService.KPI_RECORD_ENTITY_TYPE),
            isNull(),
            eq(KpiRecordService.ST_EDITING),
            eq("record"),
            eq(ACTOR_ID),
            eq(99L),
            any(Date.class));
    }
}
