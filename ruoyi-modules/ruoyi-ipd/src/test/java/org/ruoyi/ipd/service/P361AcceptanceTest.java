package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.dto.ContributionSaveReq;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-6.1 五维贡献评定表与审核（验收测试 / BR-INC-09 / AC-INC-25~28）。
 *
 * <p>本卡面统一收口 7 项验收 AC，每项 1 个 @Test，断言与卡面 AC 一一映射：
 * <ol>
 *   <li>AC-INC-27：五维度权重和 25+25+20+20+10 = 100</li>
 *   <li>AC-INC-25：市场 65% ⇒ 研发自动联动 35%（两者之和恒为 100%）</li>
 *   <li>AC-INC-26：市场 70% ⇒ 越界拒绝，错误码 CONTRIB_TIER_OUT_OF_RANGE</li>
 *   <li>AC-INC-28：仅 G5 上市后 90 天复盘阶段开放（status=LIFECYCLE/POST_LAUNCH）</li>
 *   <li>AC-INC-28：非 G5 阶段项目永远关闭（status=CONCEPT ⇒ CONTRIB_NOT_G5_STAGE）</li>
 *   <li>AC-INC-28：双 PM 自评缺一不可（仅 MARKET_PM 完成 ⇒ status 保持 DRAFT）</li>
 *   <li>A5（无"评审上级"角色）：仅各自产品组长（GROUP_LEADER）可 confirm；非组长 ⇒ 403</li>
 * </ol>
 *
 * <p>实现见 {@link ContributionService}（P3-6.2）；本卡只验收、不可改业务代码。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P361AcceptanceTest {

    @Mock private ContributionMapper contributionMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission ipdPermission;

    private ContributionService service;

    /* ============================================================
     *  AC-INC-27  五维度权重和 = 100
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-27] 五维权重和 25+25+20+20+10 = 100 ⇒ 校验通过")
    void AC_INC_27_维度权重和_100_通过() {
        // Given：标准权重 25/25/20/20/10
        // When / Then：validateDimensionWeights 不抛异常
        ContributionService.validateDimensionWeights(
            Arrays.asList(new BigDecimal("25"), new BigDecimal("25"),
                new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("10")));
    }

    /* ============================================================
     *  AC-INC-25  市场 65% ⇒ 研发自动联动 35%
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-25] 市场 65% ⇒ 研发自动联动 35%（两者之和 = 100%）")
    void AC_INC_25_市场65_研发自动35() {
        // Given：市场 PM 比例 = 0.65（区间上界）
        BigDecimal marketShare = new BigDecimal("0.65");

        // When：saveSelf 落库时调用 adjustMarketShare 路径
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        Contribution existing = baseContribution(Contribution.ST_DRAFT,
            new BigDecimal("0.55"), new BigDecimal("0.45"));
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        // Then：落库后 marketShare=0.65, rdShare=0.35（联动）
        var view = service.adjustMarketShare(PROJECT_ID, marketShare);
        assertThat(view.marketShare()).isEqualByComparingTo(new BigDecimal("0.65"));
        assertThat(view.rdShare()).isEqualByComparingTo(new BigDecimal("0.35"));
        // 两者之和恒为 1.00
        assertThat(view.marketShare().add(view.rdShare()))
            .isEqualByComparingTo(BigDecimal.ONE);
        verify(contributionMapper).updateById(any(Contribution.class));
    }

    /* ============================================================
     *  AC-INC-26  市场 70% ⇒ 越界拒绝
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-26] 市场 70% ⇒ 越界拒绝 CONTRIB_TIER_OUT_OF_RANGE（区间 40%-65%）")
    void AC_INC_26_市场70_越界拒绝() {
        // Given：市场 PM 比例 = 0.70（> 上界 0.65）
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        // When / Then：抛 IpdBusinessException，code = CONTRIB_TIER_OUT_OF_RANGE
        assertThatThrownBy(() -> service.adjustMarketShare(PROJECT_ID, new BigDecimal("0.70")))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.CONTRIB_TIER_OUT_OF_RANGE);
    }

    /* ============================================================
     *  AC-INC-28  仅 G5 上市后 90 天复盘阶段开放
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-28] LIFECYCLE 阶段项目（已上市）⇒ 评定入口开放")
    void AC_INC_28_G5_上市后90天_开放() {
        // Given：项目 status = LIFECYCLE（已上市，进入 90 天复盘窗口）
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(contributionMapper.selectOne(any())).thenReturn(null);
        when(contributionMapper.insert(any(Contribution.class))).thenAnswer(inv -> {
            Contribution c = inv.getArgument(0);
            c.setId(999L);
            return 1;
        });

        // When：MARKET_PM 自评保存
        var view = service.saveSelf(PROJECT_ID, marketSaveReq());

        // Then：成功落库（无 G5 阶段门禁错误）
        assertThat(view).isNotNull();
        assertThat(view.status()).isIn(Contribution.ST_DRAFT, Contribution.ST_SUBMITTED);
    }

    /* ============================================================
     *  AC-INC-28  非 G5 项目永远关闭
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-28] 非 G5 阶段项目（CONCEPT）⇒ 永远关闭 CONTRIB_NOT_G5_STAGE")
    void AC_INC_28_非G5项目_永远关闭() {
        // Given：项目 status = CONCEPT（未进入 G5）
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(conceptProject());

        // When / Then：抛 IpdBusinessException，code = CONTRIB_NOT_G5_STAGE
        assertThatThrownBy(() -> service.saveSelf(PROJECT_ID, marketSaveReq()))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.CONTRIB_NOT_G5_STAGE);
    }

    /* ============================================================
     *  AC-INC-28  双 PM 自评缺一不可
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-28] 仅 MARKET_PM 完成（缺 RD_PM）⇒ status 保持 DRAFT")
    void AC_INC_28_双PM自评_缺一不可() {
        // Given：项目已存在 DRAFT 记录，MARKET_PM 自评已完成（缺 RD_PM）
        when(ipdPermission.requireInternal()).thenReturn(marketPmActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = baseContribution(Contribution.ST_DRAFT,
            new BigDecimal("0.55"), new BigDecimal("0.45"));
        // MARKET_PM 五维度已落库
        existing.setMarketSelfInitiation(new BigDecimal("80"));
        existing.setMarketSelfInnovation(new BigDecimal("90"));
        existing.setMarketSelfLaunch(new BigDecimal("70"));
        existing.setMarketSelfMarketResult(new BigDecimal("85"));
        existing.setMarketSelfLeadership(new BigDecimal("95"));
        // RD_PM 五维度全部 null
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        // When：MARKET_PM 再次保存（覆盖自评）
        var view = service.saveSelf(PROJECT_ID, marketSaveReq());

        // Then：status 仍为 DRAFT（RD_PM 缺一不可 ⇒ 不升 SUBMITTED）
        assertThat(view.status()).isEqualTo(Contribution.ST_DRAFT);
    }

    /* ============================================================
     *  A5（无"评审上级"角色）各自产品组长评定
     * ============================================================ */

    @Test
    @DisplayName("[A5] 非产品组长（SUPER_ADMIN 之外的 MARKET_PM）调用 confirm ⇒ 403")
    void A5_非产品组长_无法confirm() {
        // Given：MARKET_PM 角色尝试 confirm（非 GROUP_LEADER / SUPER_ADMIN）
        when(ipdPermission.requireLeaderOrAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));

        // When / Then：抛 IpdPermissionException（403 FORBIDDEN）
        assertThatThrownBy(() -> service.confirm(PROJECT_ID, "APPROVE", null))
            .isInstanceOf(IpdPermissionException.class);
    }

    @Test
    @DisplayName("[A5] 产品组长（GROUP_LEADER）调用 confirm ⇒ APPROVE 落库 + 审计")
    void A5_产品组长_confirm通过() {
        // Given：产品组长 SUBMITTED 状态记录
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(groupLeaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());

        Contribution existing = baseContribution(Contribution.ST_SUBMITTED,
            new BigDecimal("0.55"), new BigDecimal("0.45"));
        existing.setTierCoefficient(new BigDecimal("0.83"));
        when(contributionMapper.selectOne(any())).thenReturn(existing);

        // When：产品组长 APPROVE
        var view = service.confirm(PROJECT_ID, "APPROVE", "评定通过");

        // Then：status ⇒ CONFIRMED，leaderDecision ⇒ APPROVE，审计落地
        assertThat(view.status()).isEqualTo(Contribution.ST_CONFIRMED);
        assertThat(view.leaderDecision()).isEqualTo("APPROVE");
        verify(auditLogService).append(any());
    }

    /* ============================================================
     *  Fixture
     * ============================================================ */

    private static final Long MARKET_PM_ID = 1001L;
    private static final Long RD_PM_ID = 1002L;
    private static final Long GROUP_LEADER_ID = 2001L;
    private static final Long PROJECT_ID = 500L;

    @BeforeEach
    void setUp() {
        service = new ContributionService(contributionMapper, projectMapper,
            auditLogService, ipdPermission);
    }

    private IpdActor marketPmActor() {
        return new IpdActor(MARKET_PM_ID, "M", "MARKET_PM", 10L);
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

    private Contribution baseContribution(String status, BigDecimal market, BigDecimal rd) {
        return Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(status)
            .marketShare(market).rdShare(rd)
            .delFlag("0").build();
    }

    private ContributionSaveReq marketSaveReq() {
        return new ContributionSaveReq("MARKET_PM",
            new BigDecimal("80"), new BigDecimal("90"),
            new BigDecimal("70"), new BigDecimal("85"),
            new BigDecimal("95"), "市场自评备注");
    }
}
