package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.ProjectMember;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-3.3 津贴内部台账幂等、移交与退出月份归属 验收测试
 *
 * <p>由于 mappers 是接口，Mockito 在 Java 17 上无法直接 mock；
 * 这里使用 spy + 自定义 LambdaQueryWrapper 返回值的方式构造测试场景。
 * 主要验证：(1) 月份归属逻辑（纯算法），(2) 仅台账无支付。
 */
@Tag("dev")
class P333AcceptanceTest {

    // 使用 mock 但在 setter 注入，避免构造器 final 类问题
    private static org.ruoyi.ipd.mapper.AllowanceLedgerMapper mockAllowanceLedgerMapper;
    private static org.ruoyi.ipd.mapper.ProjectMemberMapper mockProjectMemberMapper;
    private static AllowanceService allowanceService;

    private static final Long PERSON_ID = 100L;
    private static final Long PROJECT_ID = 200L;
    private static final String MONTH = "2026-09";

    @BeforeAll
    static void init() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p333-test"),
            AllowanceLedger.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p333-test"),
            ProjectMember.class);
        mockAllowanceLedgerMapper = mock(org.ruoyi.ipd.mapper.AllowanceLedgerMapper.class);
        mockProjectMemberMapper = mock(org.ruoyi.ipd.mapper.ProjectMemberMapper.class);
        allowanceService = new AllowanceService(mockAllowanceLedgerMapper, mockProjectMemberMapper);
    }

    private static Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== AC-INC: 台账幂等 (personId, projectId, month) 唯一 ====================

    @Test
    @DisplayName("幂等: 已存在台账 ⇒ existsByKey=true，不写第二条")
    void existsByKeyTrue() {
        when(mockAllowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThat(allowanceService.existsByKey(PERSON_ID, PROJECT_ID, MONTH)).isTrue();
    }

    @Test
    @DisplayName("幂等: 不存在台账 ⇒ existsByKey=false")
    void existsByKeyFalse() {
        when(mockAllowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThat(allowanceService.existsByKey(PERSON_ID, PROJECT_ID, MONTH)).isFalse();
    }

    @Test
    @DisplayName("幂等插入: 已存在 ⇒ 返回 null，不调 insert")
    void idempotentInsertSkip() {
        when(mockAllowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        AllowanceLedger ledger = AllowanceLedger.builder()
            .personId(PERSON_ID).projectId(PROJECT_ID).month(MONTH)
            .finalAmount(new BigDecimal("5000")).lockedLevel("L3")
            .baseAmount(new BigDecimal("5000")).capApplied("0")
            .build();
        AllowanceLedger result = allowanceService.idempotentInsert(ledger);
        assertThat(result).isNull();
        verify(mockAllowanceLedgerMapper, never()).insert(any(AllowanceLedger.class));
    }

    @Test
    @DisplayName("幂等插入: 不存在 ⇒ 调 insert，返回 ledger")
    void idempotentInsertRun() {
        when(mockAllowanceLedgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        AllowanceLedger ledger = AllowanceLedger.builder()
            .personId(PERSON_ID).projectId(PROJECT_ID).month(MONTH)
            .finalAmount(new BigDecimal("5000")).lockedLevel("L3")
            .baseAmount(new BigDecimal("5000")).capApplied("0")
            .build();
        AllowanceLedger result = allowanceService.idempotentInsert(ledger);
        assertThat(result).isNotNull();
        verify(mockAllowanceLedgerMapper, times(1)).insert(any(AllowanceLedger.class));
    }

    // ==================== AC-INC: 月中移交当月按月初人员 ====================

    @Test
    @DisplayName("移交: joinDate=月初前 (2026-08-30) ⇒ 2026-09 月按此成员计")
    void joinedBeforeMonthEffective() {
        Date join = parseDate("2026-08-30");
        assertThat(allowanceService.isMemberEffectiveInMonth(join, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("移交: joinDate=月初 (2026-09-01) ⇒ 2026-09 月按此成员计（含）")
    void joinedAtMonthStartEffective() {
        Date join = parseDate("2026-09-01");
        assertThat(allowanceService.isMemberEffectiveInMonth(join, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("移交: joinDate=月中 (2026-09-15) ⇒ 2026-09 月不计，新人次月生效")
    void joinedMidMonthNotEffective() {
        Date join = parseDate("2026-09-15");
        assertThat(allowanceService.isMemberEffectiveInMonth(join, "2026-09")).isFalse();
    }

    @Test
    @DisplayName("移交: joinDate=月末 (2026-09-30) ⇒ 2026-09 月不计")
    void joinedEndOfMonthNotEffective() {
        Date join = parseDate("2026-09-30");
        assertThat(allowanceService.isMemberEffectiveInMonth(join, "2026-09")).isFalse();
    }

    @Test
    @DisplayName("移交: joinDate=null ⇒ 不计（无加入日期）")
    void joinedNullNotEffective() {
        assertThat(allowanceService.isMemberEffectiveInMonth(null, "2026-09")).isFalse();
    }

    // ==================== AC-INC: 退出次月停发 ====================

    @Test
    @DisplayName("退出: exitDate=null ⇒ 活跃")
    void activeWhenNoExit() {
        assertThat(allowanceService.isMemberActiveInMonth(null, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("退出: exitDate=月中 (2026-09-15) ⇒ 2026-09 月仍计（次月起停）")
    void exitedMidMonthStillActive() {
        Date exit = parseDate("2026-09-15");
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("退出: exitDate=月末 (2026-09-30) ⇒ 2026-09 月仍计（次月起停）")
    void exitedEndOfMonthStillActive() {
        Date exit = parseDate("2026-09-30");
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("退出: exitDate=次月初 (2026-10-01) ⇒ 2026-09 月仍 active（10 月才停）")
    void exitedNextMonthStartNotActive() {
        Date exit = parseDate("2026-10-01");
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-10")).isTrue();
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-11")).isFalse();
    }

    @Test
    @DisplayName("退出: exitDate=下月之前 (2026-08-30) ⇒ 2026-09 月不活跃（次月已停）")
    void exitedBeforeMonthNotActive() {
        Date exit = parseDate("2026-08-30");
        assertThat(allowanceService.isMemberActiveInMonth(exit, "2026-09")).isFalse();
    }

    // ==================== AC-INC: 综合判定 isMemberActiveForMonth ====================

    @Test
    @DisplayName("综合: 未退出 + 已加入 ⇒ 活跃")
    void activeMemberActive() {
        ProjectMember m = ProjectMember.builder()
            .personId(PERSON_ID).projectId(PROJECT_ID)
            .joinDate(parseDate("2026-08-30"))
            .exitDate(null)
            .build();
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("综合: 月中移交新人 (2026-09-15 加入) ⇒ 9 月不活跃，10 月活跃")
    void transferredMemberActiveNextMonth() {
        ProjectMember m = ProjectMember.builder()
            .personId(PERSON_ID).projectId(PROJECT_ID)
            .joinDate(parseDate("2026-09-15"))
            .exitDate(null)
            .build();
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-09")).isFalse();
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-10")).isTrue();
    }

    @Test
    @DisplayName("综合: 退出 (2026-09-30) ⇒ 9 月仍 active（次月 10 月起停）")
    void exitedMemberNotActive() {
        ProjectMember m = ProjectMember.builder()
            .personId(PERSON_ID).projectId(PROJECT_ID)
            .joinDate(parseDate("2026-01-15"))
            .exitDate(parseDate("2026-09-30"))
            .build();
        // 退出当月仍 active
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-09")).isTrue();
        // 8 月时仍 active
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-08")).isTrue();
        // 10 月起停
        assertThat(allowanceService.isMemberActiveForMonth(m, "2026-10")).isFalse();
    }

    @Test
    @DisplayName("综合: member=null ⇒ 不活跃")
    void nullMemberNotActive() {
        assertThat(allowanceService.isMemberActiveForMonth(null, "2026-09")).isFalse();
    }

    // ==================== AC-INC: 不按日分摊 ====================

    @Test
    @DisplayName("不按日: 月中 9/15 加入 vs 月末 9/30 加入，9 月均为不活跃（不按日折算）")
    void noDailyProration() {
        Date mid = parseDate("2026-09-15");
        Date end = parseDate("2026-09-30");
        assertThat(allowanceService.isMemberEffectiveInMonth(mid, "2026-09")).isFalse();
        assertThat(allowanceService.isMemberEffectiveInMonth(end, "2026-09")).isFalse();
        assertThat(allowanceService.isMemberEffectiveInMonth(mid, "2026-10")).isTrue();
        assertThat(allowanceService.isMemberEffectiveInMonth(end, "2026-10")).isTrue();
    }

    // ==================== AC-INC: 仅内部台账不付款 ====================

    @Test
    @DisplayName("仅台账: service 层不暴露 payment/pay/disburse 方法")
    void noPaymentMethod() {
        boolean hasPayment = java.util.Arrays.stream(AllowanceService.class.getDeclaredMethods())
            .anyMatch(m -> {
                String n = m.getName().toLowerCase();
                return n.contains("pay") || n.contains("disburse") || n.contains("settle");
            });
        assertThat(hasPayment).isFalse();
    }
}
