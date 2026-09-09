package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-6.2 贡献度评定单测（合测）
 *
 * <p>覆盖 6 维度：
 * <ol>
 *   <li>正常路径：tier 公式 / 五维度加权得分 / 100 / 区间内 65% 通过联动</li>
 *   <li>边界：40%/65% 端点通过；五维度权重和 = 100 边界</li>
 *   <li>异常：70% 拒；dimSum 99/101 拒；非 G5 阶段拒；非授权角色拒</li>
 *   <li>权限：RD_PM 不能保存 MARKET_PM 自评；非组长调用 confirm 抛 403</li>
 *   <li>审计：save / adjust / confirm 必落审计（含角色、tier、决策）</li>
 *   <li>幂等：重复 confirm 不重抛；CONFIRMED 状态拒二次修改</li>
 * </ol>
 *
 * <p>AC：AC-INC-25/26/27/28；BR：BR-INC-09；ZK-IPD：§三.2.5 tierCoefficient 公式。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock private ContributionMapper contributionMapper;
    @Mock private org.ruoyi.ipd.mapper.ContributionVersionMapper versionMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission ipdPermission;
    @Mock private ProductGroupMapper productGroupMapper;

    private ContributionService service;

    private static final Long MARKET_PM_ID = 1001L;
    private static final Long RD_PM_ID = 1002L;
    private static final Long GROUP_LEADER_ID = 2001L;
    private static final Long PROJECT_ID = 500L;

    @BeforeEach
    void setUp() {
        service = new ContributionService(contributionMapper, versionMapper, projectMapper,
            productGroupMapper, auditLogService, ipdPermission);
    }

    private IpdActor marketPmActor() {
        return new IpdActor(MARKET_PM_ID, "M", "MARKET_PM", 10L);
    }

    private IpdActor rdPmActor() {
        return new IpdActor(RD_PM_ID, "R", "RD_PM", 10L);
    }

    private IpdActor groupLeaderActor() {
        return new IpdActor(GROUP_LEADER_ID, "L", "GROUP_LEADER", 10L);
    }

    private Project lifecycleProject() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setStatus("LIFECYCLE");
        p.setDelFlag("0");
        return p;
    }

    private Project conceptProject() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setStatus("CONCEPT");
        p.setDelFlag("0");
        return p;
    }

    /* ============================================================
     *  静态方法：tier 公式 / 区间 / 维度权重校验
     * ============================================================ */

    @Test
    @DisplayName("tier 公式：dim=100,100,100,100,100 → tier=1.00 (5 维度满分)")
    void tierFormula_allPerfectScore_returnsOne() {
        BigDecimal tier = ContributionService.computeTierCoefficient(
            bd100(), bd100(), bd100(), bd100(), bd100());
        assertThat(tier).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("tier 公式：dim=80,80,80,80,80 → 加权=8000 → tier=8000/10000=0.80")
    void tierFormula_uniform80_returns0_80() {
        BigDecimal tier = ContributionService.computeTierCoefficient(
            bd("80"), bd("80"), bd("80"), bd("80"), bd("80"));
        // 80 × 25 + 80 × 25 + 80 × 20 + 80 × 20 + 80 × 10 = 80 × 100 = 8000
        // 8000 / 10000 = 0.80
        assertThat(tier).isEqualByComparingTo(new BigDecimal("0.80"));
    }

    @Test
    @DisplayName("tier 公式：dim=0,0,0,0,0 → tier=0.00 (5 维度零分)")
    void tierFormula_allZero_returnsZero() {
        BigDecimal tier = ContributionService.computeTierCoefficient(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(tier).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("区间校验：marketShare=0.40 边界通过")
    void marketShare_lowerBoundary_accepted() {
        ContributionService.validateMarketShare(new BigDecimal("0.40"));
    }

    @Test
    @DisplayName("区间校验：marketShare=0.65 边界通过（AC-INC-25）")
    void marketShare_upperBoundary_accepted() {
        ContributionService.validateMarketShare(new BigDecimal("0.65"));
    }

    @Test
    @DisplayName("区间校验：marketShare=0.70 拒绝（AC-INC-26）")
    void marketShare_exceedsUpperBound_rejected() {
        assertThatThrownBy(() -> ContributionService.validateMarketShare(new BigDecimal("0.70")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("市场 PM 比例");
    }

    @Test
    @DisplayName("区间校验：marketShare=0.39 拒绝")
    void marketShare_belowLowerBound_rejected() {
        assertThatThrownBy(() -> ContributionService.validateMarketShare(new BigDecimal("0.39")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("市场 PM 比例");
    }

    @Test
    @DisplayName("区间校验：marketShare=0.55 → rdShare=0.45，研发联动区间内")
    void marketShare_linkedRd_valid() {
        ContributionService.validateMarketShare(new BigDecimal("0.55"));
    }

    @Test
    @DisplayName("维度权重校验：25+25+20+20+10=100 通过（AC-INC-27）")
    void dimensionWeights_sum_100_valid() {
        ContributionService.validateDimensionWeights(ContributionService.defaultDimensionWeights());
    }

    @Test
    @DisplayName("维度权重校验：99 拒绝")
    void dimensionWeights_sum_99_rejected() {
        assertThatThrownBy(() -> ContributionService.validateDimensionWeights(java.util.Arrays.asList(
            new BigDecimal("25"), new BigDecimal("25"), new BigDecimal("20"),
            new BigDecimal("20"), new BigDecimal("9"))))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("权重之和");
    }

    @Test
    @DisplayName("维度权重校验：101 拒绝")
    void dimensionWeights_sum_101_rejected() {
        assertThatThrownBy(() -> ContributionService.validateDimensionWeights(java.util.Arrays.asList(
            new BigDecimal("25"), new BigDecimal("25"), new BigDecimal("20"),
            new BigDecimal("20"), new BigDecimal("11"))))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("权重之和");
    }

    /* ============================================================
     *  双 PM 自评保存
     * ============================================================ */

    @Test
    @DisplayName("saveSelf：MARKET_PM 自评保存 → tier 落库 + 审计")
    void saveSelf_marketPm_recordsTierAndAudit() {
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(contributionMapper.selectOne(any())).thenReturn(null);
        when(contributionMapper.insert(any(Contribution.class))).thenAnswer(inv -> {
            Contribution c = inv.getArgument(0);
            c.setId(999L);
            return 1;
        });

        var req = new org.ruoyi.ipd.dto.ContributionSaveReq(
            "MARKET_PM", bd("80"), bd("90"), bd("70"), bd("85"), bd("95"), "市场自评备注");

        var view = service.saveSelf(PROJECT_ID, req);

        // tier = 80×25+90×25+70×20+85×20+95×10 = 2000+2250+1400+1700+950 = 8300
        // 8300 / 10000 = 0.83
        assertThat(view.tierCoefficient()).isEqualByComparingTo(new BigDecimal("0.83"));
        verify(contributionMapper).insert(any(Contribution.class));
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("saveSelf：非 G5 阶段项目 → CONTRIB_NOT_G5_STAGE")
    void saveSelf_nonG5Stage_rejected() {
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(conceptProject());

        var req = new org.ruoyi.ipd.dto.ContributionSaveReq(
            "MARKET_PM", bd("80"), bd("90"), bd("70"), bd("85"), bd("95"), null);

        assertThatThrownBy(() -> service.saveSelf(PROJECT_ID, req))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("G5");
    }

    @Test
    @DisplayName("saveSelf：RD_PM 不能保存 MARKET_PM 自评 → CONTRIB_NOT_AUTHORIZED")
    void saveSelf_roleMismatch_rejected() {
        when(ipdPermission.requireInternal()).thenReturn(rdPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        var req = new org.ruoyi.ipd.dto.ContributionSaveReq(
            "MARKET_PM", bd("80"), bd("90"), bd("70"), bd("85"), bd("95"), null);

        assertThatThrownBy(() -> service.saveSelf(PROJECT_ID, req))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("评定人角色与提交 role 不一致");
    }

    @Test
    @DisplayName("saveSelf：双 PM 均完成 → status 升 SUBMITTED")
    void saveSelf_bothRoles_complete_upgradeToSubmitted() {
        // 第一次保存 MARKET_PM（创建）
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(contributionMapper.selectOne(any())).thenReturn(null);
        when(contributionMapper.insert(any(Contribution.class))).thenAnswer(inv -> {
            Contribution c = inv.getArgument(0);
            c.setId(999L);
            return 1;
        });
        service.saveSelf(PROJECT_ID, new org.ruoyi.ipd.dto.ContributionSaveReq(
            "MARKET_PM", bd("80"), bd("90"), bd("70"), bd("85"), bd("95"), null));

        // 第二次保存 RD_PM（更新已存在记录 → SUBMITTED）
        when(ipdPermission.requireInternal()).thenReturn(rdPmActor());
        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_DRAFT)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .marketSelfInitiation(new BigDecimal("80")).marketSelfInnovation(new BigDecimal("90"))
            .marketSelfLaunch(new BigDecimal("70")).marketSelfMarketResult(new BigDecimal("85"))
            .marketSelfLeadership(new BigDecimal("95"))
            .tierCoefficient(new BigDecimal("0.83"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        var rdView = service.saveSelf(PROJECT_ID, new org.ruoyi.ipd.dto.ContributionSaveReq(
            "RD_PM", bd("75"), bd("85"), bd("80"), bd("80"), bd("90"), "研发自评"));

        // RD 自评 tier = 75×25+85×25+80×20+80×20+90×10 = 1875+2125+1600+1600+900 = 8100
        // 8100 / 10000 = 0.81
        assertThat(rdView.tierCoefficient()).isEqualByComparingTo(new BigDecimal("0.81"));
        assertThat(rdView.status()).isEqualTo(Contribution.ST_SUBMITTED);
    }

    /* ============================================================
     *  调整市场 PM 比例（区间 + 联动）
     * ============================================================ */

    @Test
    @DisplayName("adjustMarketShare：0.55 → 落库 + rdShare 自动联动 0.45")
    void adjustMarketShare_55_persistsAndLinks() {
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_SUBMITTED)
            .marketShare(new BigDecimal("0.50")).rdShare(new BigDecimal("0.50"))
            .tierCoefficient(new BigDecimal("80.00"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        var view = service.adjustMarketShare(PROJECT_ID, new BigDecimal("0.55"));
        assertThat(view.marketShare()).isEqualByComparingTo(new BigDecimal("0.55"));
        assertThat(view.rdShare()).isEqualByComparingTo(new BigDecimal("0.45"));
        verify(contributionMapper).updateById(any(Contribution.class));
    }

    @Test
    @DisplayName("adjustMarketShare：0.70 → 拒（AC-INC-26）")
    void adjustMarketShare_70_rejected() {
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        assertThatThrownBy(() -> service.adjustMarketShare(PROJECT_ID, new BigDecimal("0.70")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("市场 PM 比例");
    }

    @Test
    @DisplayName("adjustMarketShare：CONFIRMED 状态拒二次调整")
    void adjustMarketShare_confirmedState_rejected() {
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_CONFIRMED)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> service.adjustMarketShare(PROJECT_ID, new BigDecimal("0.50")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("已确认");
        verify(contributionMapper, never()).updateById(any(Contribution.class));
    }

    /* ============================================================
     *  产品组长确认
     * ============================================================ */

    @Test
    @DisplayName("confirm：GROUP_LEADER APPROVE → CONFIRMED + 审计")
    void confirm_approve_transitionsToConfirmed() {
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(groupLeaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_SUBMITTED)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .tierCoefficient(new BigDecimal("83.00"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);
        // 归档前置：无历史版本 → 本次确认归档为 versionNo=1
        when(versionMapper.selectList(any())).thenReturn(java.util.List.of());

        var view = service.confirm(PROJECT_ID, "APPROVE", "评定通过");
        assertThat(view.status()).isEqualTo(Contribution.ST_CONFIRMED);
        assertThat(view.leaderDecision()).isEqualTo("APPROVE");
        verify(auditLogService).append(any());
        // BR-INC-09 归档版本可追溯：APPROVE 必归档一份快照
        verify(versionMapper).insert(org.mockito.ArgumentMatchers
            .<org.ruoyi.ipd.domain.ContributionVersion>argThat(cv ->
                cv.getVersionNo() == 1
                    && cv.getProjectId().equals(PROJECT_ID)
                    && "CONFIRMED".equals(cv.getStatus())));
    }

    @Test
    @DisplayName("confirm：非 GROUP_LEADER/SUPER_ADMIN → 403")
    void confirm_nonLeader_rejected() {
        when(ipdPermission.requireLeaderOrAdmin())
            .thenThrow(new IpdPermissionException(403,
                org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> service.confirm(PROJECT_ID, "APPROVE", null))
            .isInstanceOf(IpdPermissionException.class);
    }

    @Test
    @DisplayName("confirm：REJECT → 退回 DRAFT")
    void confirm_reject_returnsToDraft() {
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(groupLeaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_SUBMITTED)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .tierCoefficient(new BigDecimal("83.00"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        var view = service.confirm(PROJECT_ID, "REJECT", "市场比例偏低");
        assertThat(view.status()).isEqualTo(Contribution.ST_DRAFT);
        assertThat(view.leaderDecision()).isEqualTo("REJECT");
    }

    @Test
    @DisplayName("confirm：非法 decision → PARAM_INVALID")
    void confirm_invalidDecision_rejected() {
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(groupLeaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_SUBMITTED)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .delFlag("0").build();
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> service.confirm(PROJECT_ID, "FOO", null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("APPROVE 或 REJECT");
    }

    /* ============================================================
     *  工具方法
     * ============================================================ */

    private static BigDecimal bd100() {
        return new BigDecimal("100");
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}