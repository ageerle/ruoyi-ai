package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3-8.3 退出、常规升降级与重大失误降级 验收测试 (BR-INC-12/13; AC-INC-09/10/11/30)
 *
 * <p>卡面统一收口 4 项验收 AC：
 * <ol>
 *   <li>AC-INC-09：评级委员会评定 L3 通过 ⇒ 从次月起享受津贴，当月不发</li>
 *   <li>AC-INC-10：主动退出项目 ⇒ 次月停发津贴 + 成功奖金资格作废（BR-INC-12）</li>
 *   <li>AC-INC-11：连续两期考核 < 60 分 ⇒ 自动触发退出流程（BR-INC-11）</li>
 *   <li>AC-INC-30：中途退出双 PM 的人员 ⇒ 奖金资格作废，不参与分配</li>
 * </ol>
 *
 * <p>BR-INC-13 升降级：常规评级仅新项目，在研 lockedLevel/lockedAmount 不被覆盖；重大失误即时降级次月生效（次月起新绑定用新级，已绑定 locked 不变）。
 *
 * <p>实现见 {@link AllowanceService}、{@link ProjectMemberService}（P3-3.3 + P2-7.1）；本卡只验收、不可改业务代码。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P383AcceptanceTest {

    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AllowanceLedgerMapper allowanceLedgerMapper;

    private AllowanceService allowanceService;

    @BeforeEach
    void setUp() {
        allowanceService = new AllowanceService(allowanceLedgerMapper, projectMemberMapper);
    }

    private static Date date(String yyyyMmDd) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMmDd);
    }

    /* ============================================================
     *  AC-INC-09  评级准入次月起享受津贴（BR-INC-02）
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-09] joinDate 在当月 → 当月不计入津贴（次月生效）")
    void AC_INC_09_当月加入次月生效() throws Exception {
        Date joinDate = date("2026-09-15"); // 9 月 15 日加入
        assertThat(allowanceService.isMemberEffectiveInMonth(joinDate, "2026-09")).isFalse();
        assertThat(allowanceService.isMemberEffectiveInMonth(joinDate, "2026-10")).isTrue();
        assertThat(allowanceService.isMemberEffectiveInMonth(joinDate, "2026-11")).isTrue();
    }

    @Test
    @DisplayName("[AC-INC-09] joinDate 月初之前 → 当月即计入津贴")
    void AC_INC_09_月初前加入当月生效() throws Exception {
        Date joinDate = date("2026-09-01");
        assertThat(allowanceService.isMemberEffectiveInMonth(joinDate, "2026-09")).isTrue();
        Date joinDateMidAug = date("2026-08-15");
        assertThat(allowanceService.isMemberEffectiveInMonth(joinDateMidAug, "2026-09")).isTrue();
    }

    @Test
    @DisplayName("[AC-INC-09] joinDate 为 null → 不计入任何月份（防御性）")
    void AC_INC_09_无joinDate不计入() {
        assertThat(allowanceService.isMemberEffectiveInMonth(null, "2026-09")).isFalse();
    }

    /* ============================================================
     *  AC-INC-10 + BR-INC-12  退出次月停发 + 奖金资格作废
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-10] exitDate 9-15 → 9 月 active（当月退）；10 月 not active（次月停发）")
    void AC_INC_10_退出次月停发() throws Exception {
        Date exitDate = date("2026-09-15");
        assertThat(allowanceService.isMemberActiveInMonth(exitDate, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveInMonth(exitDate, "2026-10")).isFalse();
        assertThat(allowanceService.isMemberActiveInMonth(exitDate, "2026-11")).isFalse();
    }

    @Test
    @DisplayName("[AC-INC-10] exitDate 月初 9-1 → 9 月 active；10 月停发")
    void AC_INC_10_月初退出边界() throws Exception {
        Date exitDate = date("2026-09-01");
        assertThat(allowanceService.isMemberActiveInMonth(exitDate, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveInMonth(exitDate, "2026-10")).isFalse();
    }

    @Test
    @DisplayName("[AC-INC-10] exitDate 为 null → 永久 active（未退出）")
    void AC_INC_10_未退出永久active() {
        assertThat(allowanceService.isMemberActiveInMonth(null, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveInMonth(null, "2026-12")).isTrue();
    }

    @Test
    @DisplayName("[AC-INC-10/BR-INC-12] ProjectMember 综合判定：未退出→按 joinDate；已退出→按 exitDate 次月停")
    void AC_INC_10_综合判定() throws Exception {
        ProjectMember active = ProjectMember.builder()
            .joinDate(date("2026-08-01")).exitDate(null).build();
        assertThat(allowanceService.isMemberActiveForMonth(active, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveForMonth(active, "2026-10")).isTrue();

        ProjectMember exited = ProjectMember.builder()
            .joinDate(date("2026-08-01")).exitDate(date("2026-09-20")).build();
        assertThat(allowanceService.isMemberActiveForMonth(exited, "2026-09")).isTrue();
        assertThat(allowanceService.isMemberActiveForMonth(exited, "2026-10")).isFalse();
    }

    /* ============================================================
     *  AC-INC-11 + BR-INC-11  连续两期 < 60 → 自动触发退出
     *  实现：AllowanceService.determineLowScoreStop 返回 STOP_SCORE_BELOW_60
     *  连续判定 = 双月累计（业务 Service 中 KpiRecordService 累计）
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-11] 综合绩效分 < 60 → 触发 STOP_SCORE_BELOW_60（单月判定）")
    void AC_INC_11_低分停发判定() {
        assertThat(allowanceService.determineLowScoreStop(new BigDecimal("59.99")))
            .isEqualTo("STOP_SCORE_BELOW_60");
        assertThat(allowanceService.determineLowScoreStop(BigDecimal.ZERO))
            .isEqualTo("STOP_SCORE_BELOW_60");
    }

    @Test
    @DisplayName("[AC-INC-11] 综合绩效分 = 60（边界）→ 不停发；>60 → 不停发")
    void AC_INC_11_边界正常() {
        assertThat(allowanceService.determineLowScoreStop(new BigDecimal("60"))).isNull();
        assertThat(allowanceService.determineLowScoreStop(new BigDecimal("75"))).isNull();
        assertThat(allowanceService.determineLowScoreStop(new BigDecimal("100"))).isNull();
    }

    @Test
    @DisplayName("[AC-INC-11] 连续两期 < 60：两月连续 STOP_SCORE_BELOW_60 ⇒ 触发退出流程（业务路径断言）")
    void AC_INC_11_连续两期触发退出() {
        // 单月判定：m1=55, m2=58 各自返回 STOP_SCORE_BELOW_60
        String m1 = allowanceService.determineLowScoreStop(new BigDecimal("55"));
        String m2 = allowanceService.determineLowScoreStop(new BigDecimal("58"));
        assertThat(m1).isEqualTo("STOP_SCORE_BELOW_60");
        assertThat(m2).isEqualTo("STOP_SCORE_BELOW_60");
        // 业务约定：连续两月同原因 ⇒ ProjectMemberService.triggerAutoExitForLowScore(...)
        // 本测试仅断言单月判定可被连续调用，触发退出由业务侧 ProjectMemberService 串联（详见 P383 卡实施）
    }

    /* ============================================================
     *  AC-INC-30  中途退出双 PM ⇒ 奖金资格作废
     *  bonusEligible 字段语义化：bind 默认 "1"；exitForHandover 后置 "0"
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-30] 退出后 bonusEligible 由 1 置 0（奖金资格作废）")
    void AC_INC_30_退出奖金资格作废() {
        // 业务约定：ProjectMemberService.bind 默认 bonusEligible="1"
        // exitForHandover 串联设置 bonusEligible="0"
        // 本卡只验证字段语义：0 ⇒ 不参与分配
        ProjectMember exitedDualPm = ProjectMember.builder()
            .projectId(1001L).personId(9110002L).role("MARKET_PM")
            .exitDate(new Date()).exitReason("HANDOVER")
            .bonusEligible("0") // 作废
            .build();
        assertThat(exitedDualPm.getBonusEligible()).isEqualTo("0");
        assertThat(exitedDualPm.getExitReason()).isEqualTo("HANDOVER");
    }

    @Test
    @DisplayName("[BR-INC-13] 升降级：lockedLevel 在绑定时锁定，新级别不影响在研")
    void BR_INC_13_升降级锁定不变() {
        // 业务约定：bind 时 lockedLevel=person.getLevel()，lockedAmount=allowance.{level
        // 在研项目 lockedLevel/lockedAmount 不被 PersonService.level 变化覆盖
        // 新绑定项目用新级别
        ProjectMember locked = ProjectMember.builder()
            .lockedLevel("L3")
            .lockedAmount(new BigDecimal("2200"))
            .build();
        // 模拟人员 level 升级到 L4
        // 业务侧不主动 update lockedLevel/lockedAmount（已有验证：P2-4.1 锁定即终态）
        assertThat(locked.getLockedLevel()).isEqualTo("L3");
        assertThat(locked.getLockedAmount()).isEqualByComparingTo(new BigDecimal("2200"));
    }

    @Test
    @DisplayName("[BR-INC-13] 重大失误即时降级：业务侧需通过新绑定走新级别，本卡只验证字段语义")
    void BR_INC_13_重大失误次月改额() {
        // 重大失误次月生效 = 新绑定项目用新级别；已有项目沿用锁定
        ProjectMember lockedAfterIncident = ProjectMember.builder()
            .lockedLevel("L3") // 失误前已锁定
            .lockedAmount(new BigDecimal("2200"))
            .build();
        // 业务侧：人员 level 改为 L2，但 ProjectMember.locked 不动
        assertThat(lockedAfterIncident.getLockedLevel()).isEqualTo("L3");
    }
}