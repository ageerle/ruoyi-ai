package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P3-3.1 月度津贴基础额、锁级与 2 倍封顶验收测试
 * AC: AC-INC-03/04/06/08；BR: BR-INC-02/03
 *
 * <p>Mockito 单元验收（对齐 P233 惯例）；真库 HTTP 验收另见 docs/ipd-系统说明/验收/。
 *
 * <p>不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P331AcceptanceTest {

    @Mock private AllowanceLedgerMapper allowanceLedgerMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;

    @InjectMocks private AllowanceService allowanceService;

    private static final Long PERSON_ID = 1L;
    private static final String MONTH = "2026-09";
    private static final BigDecimal L3_BASE = new BigDecimal("2000");

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p331-test"),
            AllowanceLedger.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p331-test-mem"),
            ProjectMember.class);
    }

    private ProjectMember memberWith(Long projectId, String level, BigDecimal amount) {
        return ProjectMember.builder()
            .id(projectId)
            .personId(PERSON_ID)
            .projectId(projectId)
            .lockedLevel(level)
            .lockedAmount(amount)
            .build();
    }

    // ==================== AC-INC-03/04：叠加 + 2 倍封顶 ====================

    @Test
    @DisplayName("AC-INC-03: L3 PM 绑定 4 个项目（均 L3 基准 2000）=> 2000×4=8000，封顶 2×2000=4000，实发 4000")
    void fourProjectsCappedAt2x() {
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(
                memberWith(11L, "L3", L3_BASE),
                memberWith(12L, "L3", L3_BASE),
                memberWith(13L, "L3", L3_BASE),
                memberWith(14L, "L3", L3_BASE)));
        BigDecimal result = allowanceService.calculateMonthlyAllowance(PERSON_ID, MONTH);
        assertThat(result).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("AC-INC-04: 绑定 2 个项目（均 L3 基准 2000）=> 2000×2=4000，未超封顶，全额发放")
    void twoProjectsFullAmount() {
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(
                memberWith(11L, "L3", L3_BASE),
                memberWith(12L, "L3", L3_BASE)));
        BigDecimal result = allowanceService.calculateMonthlyAllowance(PERSON_ID, MONTH);
        assertThat(result).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("AC-INC-04: 绑定 1 个项目（L3 基准 2000）=> 2000，未超封顶，全额")
    void oneProjectFullAmount() {
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(memberWith(11L, "L3", L3_BASE)));
        BigDecimal result = allowanceService.calculateMonthlyAllowance(PERSON_ID, MONTH);
        assertThat(result).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("混合评级：L5 绑定 1 + L3 绑定 3 => base=3000, sum=3000+3*2000=9000, cap=2*3000=6000, 实发 6000")
    void mixedLevelsCapFromMax() {
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(
                memberWith(11L, "L5", new BigDecimal("3000")),
                memberWith(12L, "L3", new BigDecimal("2000")),
                memberWith(13L, "L3", new BigDecimal("2000")),
                memberWith(14L, "L3", new BigDecimal("2000"))));
        BigDecimal result = allowanceService.calculateMonthlyAllowance(PERSON_ID, MONTH);
        assertThat(result).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("无项目 => 津贴 0")
    void noProjectZero() {
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());
        BigDecimal result = allowanceService.calculateMonthlyAllowance(PERSON_ID, MONTH);
        assertThat(result).isEqualByComparingTo("0");
    }

    // ==================== AC-INC-06：津贴不乘绩效系数 ====================

    @Test
    @DisplayName("AC-INC-06: 单项目津贴 = lockedAmount，不乘任何绩效系数")
    void allowanceDoesNotMultiplyByScore() {
        ProjectMember m = memberWith(11L, "L3", L3_BASE);
        BigDecimal result = allowanceService.calculateProjectAllowance(m);
        // 即使存在综合得分 90/76，津贴也是 2000 而非 2000*0.9=1800
        assertThat(result).isEqualByComparingTo("2000");
    }

    // ==================== AC-INC-08：主项目无产出不触发停发 ====================

    @Test
    @DisplayName("AC-INC-08: 主项目（isMain=true）无产出 60 天不触发停发")
    void mainProjectNoOutputDoesNotStop() {
        String reason = allowanceService.determineStopReason(null, true, true);
        assertThat(reason).isNull();
    }

    @Test
    @DisplayName("AC-INC-08: 附加项目（isMain=false）无产出 60 天触发 NO_OUTPUT_60_DAYS 停发")
    void additionalProjectNoOutputStops() {
        String reason = allowanceService.determineStopReason(null, true, false);
        assertThat(reason).isEqualTo("NO_OUTPUT_60_DAYS");
    }

    @Test
    @DisplayName("AC-INC-05: 综合得分 < 60 触发 SCORE_BELOW_60 停发（不论主/附加）")
    void scoreBelow60Stops() {
        String reasonMain = allowanceService.determineStopReason(new BigDecimal("58"), false, true);
        assertThat(reasonMain).isEqualTo("SCORE_BELOW_60");
        String reasonAdd = allowanceService.determineStopReason(new BigDecimal("58"), false, false);
        assertThat(reasonAdd).isEqualTo("SCORE_BELOW_60");
    }

    @Test
    @DisplayName("AC-INC-05: 综合得分 = 60（边界）=> 不停发")
    void score60BoundaryDoesNotStop() {
        String reason = allowanceService.determineStopReason(new BigDecimal("60"), false, true);
        assertThat(reason).isNull();
    }

    // ==================== 幂等 ====================

    @Test
    @DisplayName("幂等: 同 (personId, projectId, month) 已存在则 recordOrSkip 返回 null 不重写")
    void idempotencySkipWhenExists() {
        when(allowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        AllowanceLedger ledger = AllowanceLedger.builder()
            .personId(PERSON_ID).projectId(11L).month(MONTH)
            .finalAmount(new BigDecimal("2000")).build();
        AllowanceLedger result = allowanceService.recordOrSkip(ledger, 11L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("幂等: 不存在则写入台账并返回 ledger")
    void idempotencyInsertWhenMissing() {
        when(allowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(allowanceLedgerMapper.insert(any(AllowanceLedger.class))).thenReturn(1);
        AllowanceLedger ledger = AllowanceLedger.builder()
            .personId(PERSON_ID).projectId(11L).month(MONTH)
            .finalAmount(new BigDecimal("2000")).build();
        AllowanceLedger result = allowanceService.recordOrSkip(ledger, 11L);
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(11L);
    }

    // ==================== buildLedger：构造台账 ====================

    @Test
    @DisplayName("buildLedger: 4 项目封顶时 capApplied=1，最终额=4000")
    void buildLedgerWithCap() {
        AllowanceLedger ledger = allowanceService.buildLedger(PERSON_ID, MONTH,
            new BigDecimal("4000"), "L3", L3_BASE, true);
        assertThat(ledger.getLockedLevel()).isEqualTo("L3");
        assertThat(ledger.getBaseAmount()).isEqualByComparingTo("2000");
        assertThat(ledger.getFinalAmount()).isEqualByComparingTo("4000");
        assertThat(ledger.getCapApplied()).isEqualTo("1");
    }

    @Test
    @DisplayName("buildLedger: 2 项目未封顶时 capApplied=0")
    void buildLedgerNoCap() {
        AllowanceLedger ledger = allowanceService.buildLedger(PERSON_ID, MONTH,
            new BigDecimal("4000"), "L3", L3_BASE, false);
        assertThat(ledger.getCapApplied()).isEqualTo("0");
    }
}
