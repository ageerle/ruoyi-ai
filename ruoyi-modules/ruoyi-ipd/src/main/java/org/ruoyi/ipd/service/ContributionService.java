package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.ContributionVersion;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.dto.ContributionSaveReq;
import org.ruoyi.ipd.dto.ContributionVersionView;
import org.ruoyi.ipd.dto.ContributionView;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.mapper.ContributionVersionMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 贡献度评定服务（P3-6.2；BR-INC-09 / AC-INC-25~28；ZK-IPD §三.2.5）。
 *
 * <p>核心规则：
 * <ul>
 *   <li>五维度权重：立项主导 25 + 差异化创新 25 + 上市节奏 20 + 市场结果 20 + 协同领导力 10 = 100</li>
 *   <li>市场 PM 比例 ∈ [0.40, 0.65]；研发 PM 比例 = 1.0 - market（联动）</li>
 *   <li>tierCoefficient = 五维度加权得分 / 100（落入 BonusPoolService 4 因子叠加）</li>
 *   <li>入口仅在 G5 上市后 90 天复盘阶段开放；评定人 = 双 PM 自评 + 各自产品组长</li>
 * </ul>
 *
 * <p>依赖：复用 {@link AuditLogService} / {@link IpdPermission} / {@link ProjectService}。
 */
@Service
public class ContributionService {

    private static final Logger log = LoggerFactory.getLogger(ContributionService.class);

    private final ContributionMapper contributionMapper;
    private final ContributionVersionMapper versionMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;
    private final IpdPermission ipdPermission;

    public ContributionService(ContributionMapper contributionMapper,
                                ContributionVersionMapper versionMapper,
                                ProjectMapper projectMapper,
                                AuditLogService auditLogService,
                                IpdPermission ipdPermission) {
        this.contributionMapper = contributionMapper;
        this.versionMapper = versionMapper;
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
        this.ipdPermission = ipdPermission;
    }

    /** 五维度权重（百分制；和 = 100） */
    public static final BigDecimal W_INITIATION = new BigDecimal("25");
    public static final BigDecimal W_INNOVATION = new BigDecimal("25");
    public static final BigDecimal W_LAUNCH = new BigDecimal("20");
    public static final BigDecimal W_MARKET_RESULT = new BigDecimal("20");
    public static final BigDecimal W_LEADERSHIP = new BigDecimal("10");

    /** 市场 PM 比例下/上限（含端点） */
    public static final BigDecimal MARKET_MIN = new BigDecimal("0.40");
    public static final BigDecimal MARKET_MAX = new BigDecimal("0.65");

    /** 权重和容差（防浮点累计） */
    public static final BigDecimal WEIGHT_SUM_TOLERANCE = new BigDecimal("0.01");

    /** PM 角色白名单 */
    public static final Set<String> PM_ROLES = Set.of(Contribution.ROLE_MARKET, Contribution.ROLE_RD);

    /** 评定入口开放的阶段码（G5） */
    public static final String STAGE_CODE_G5 = "G5";

    /* ===========================================================
     *  静态校验（公开，供 Controller / 其他 Service 调用）
     * =========================================================== */

    /**
     * 校验市场 PM 比例区间：∈ [0.40, 0.65]；并联动校验研发比例 ∈ [0.35, 0.60]。
     * <p>AC-INC-26：70% 拒；AC-INC-25：65% 通过（rd 自动 35%）。
     */
    public static void validateMarketShare(BigDecimal marketShare) {
        if (marketShare == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_TIER_OUT_OF_RANGE, "市场 PM 比例不能为空");
        }
        if (marketShare.compareTo(MARKET_MIN) < 0 || marketShare.compareTo(MARKET_MAX) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_TIER_OUT_OF_RANGE,
                "市场 PM 比例必须在 [0.40, 0.65] 区间（当前=" + marketShare.toPlainString() + "）");
        }
        BigDecimal rdShare = BigDecimal.ONE.subtract(marketShare);
        BigDecimal rdMin = BigDecimal.ONE.subtract(MARKET_MAX); // 0.35
        BigDecimal rdMax = BigDecimal.ONE.subtract(MARKET_MIN); // 0.60
        if (rdShare.compareTo(rdMin) < 0 || rdShare.compareTo(rdMax) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_TIER_OUT_OF_RANGE,
                "研发 PM 联动比例必须在 [0.35, 0.60] 区间（当前=" + rdShare.toPlainString() + "）");
        }
    }

    /**
     * 校验五维度权重和：dimSum = 100 ± 0.01。
     * <p>AC-INC-27：25+25+20+20+10 = 100。
     */
    public static void validateDimensionWeights(List<BigDecimal> weights) {
        if (weights == null || weights.size() != 5) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_DIM_SUM_NOT_100,
                "贡献度五维度权重必须为五项 [立项主导, 差异化创新, 上市节奏, 市场结果, 协同领导力]");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(new BigDecimal("100")) > 0) {
                throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_DIM_SUM_NOT_100,
                    "单项权重必须在 [0, 100] 区间");
            }
            sum = sum.add(w);
        }
        BigDecimal diff = sum.subtract(new BigDecimal("100")).abs();
        if (diff.compareTo(WEIGHT_SUM_TOLERANCE) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_DIM_SUM_NOT_100,
                "五维度权重之和必须 = 100（当前=" + sum.toPlainString() + "）");
        }
    }

    /**
     * 五维度加权得分 → tierCoefficient（ZK-IPD §三.2.5 修正因子）。
     * <p>{@code tier = (dim1×25 + dim2×25 + dim3×20 + dim4×20 + dim5×10) / 10000}
     * <p>五维度全 100 分 ⇒ tier = 1.00（最高修正）；全 0 分 ⇒ tier = 0.00（无修正）。
     * 结果保留 2 位小数（HALF_UP），落入 {@code BonusPoolService} 4 因子叠加。
     */
    public static BigDecimal computeTierCoefficient(BigDecimal dimInitiation, BigDecimal dimInnovation,
                                                    BigDecimal dimLaunch, BigDecimal dimMarketResult,
                                                    BigDecimal dimLeadership) {
        BigDecimal weighted = dimInitiation.multiply(W_INITIATION)
            .add(dimInnovation.multiply(W_INNOVATION))
            .add(dimLaunch.multiply(W_LAUNCH))
            .add(dimMarketResult.multiply(W_MARKET_RESULT))
            .add(dimLeadership.multiply(W_LEADERSHIP));
        return weighted.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP);
    }

    /* ===========================================================
     *  查询 / 预览
     * =========================================================== */

    /**
     * 查询项目最新贡献度评定；若不存在则返回 null。
     */
    public ContributionView getByProject(Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        Project project = requireProject(projectId);
        requireG5Stage(project);
        Contribution c = contributionMapper.selectOne(new LambdaQueryWrapper<Contribution>()
            .eq(Contribution::getProjectId, projectId)
            .eq(Contribution::getDelFlag, "0")
            .orderByDesc(Contribution::getId)
            .last("limit 1"));
        if (c == null) return null;
        log.debug("[{}] 查询贡献度 projectId={} status={} tierCoefficient={}",
            actor.id(), projectId, c.getStatus(), c.getTierCoefficient());
        return toView(c);
    }

    /**
     * 公式预览：不改库；返回当前五维度下的 tierCoefficient 与联动比例。
     */
    public ContributionView preview(Long projectId, ContributionSaveReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        requireProject(projectId);
        validateRole(req.role());
        validateScoreRange("立项主导", req.dimInitiation());
        validateScoreRange("差异化创新", req.dimInnovation());
        validateScoreRange("上市节奏", req.dimLaunch());
        validateScoreRange("市场结果", req.dimMarketResult());
        validateScoreRange("协同领导力", req.dimLeadership());

        BigDecimal tier = computeTierCoefficient(req.dimInitiation(), req.dimInnovation(),
            req.dimLaunch(), req.dimMarketResult(), req.dimLeadership());

        log.debug("[{}] 公式预览 role={} tierCoefficient={}", actor.id(), req.role(), tier);

        BigDecimal defaultMarket = new BigDecimal("0.55");
        return ContributionView.builder()
            .projectId(projectId)
            .status(Contribution.ST_DRAFT)
            .marketShare(defaultMarket)
            .rdShare(BigDecimal.ONE.subtract(defaultMarket))
            .weightsValid(true)
            .dimInitiation(req.dimInitiation())
            .dimInnovation(req.dimInnovation())
            .dimLaunch(req.dimLaunch())
            .dimMarketResult(req.dimMarketResult())
            .dimLeadership(req.dimLeadership())
            .tierCoefficient(tier)
            .build();
    }

    /* ===========================================================
     *  双 PM 自评保存（SUBMITTED 流程节点 1）
     * =========================================================== */

    /**
     * 双 PM 自评保存（落地五维度 + tierCoefficient）。
     * <p>幂等：同 (projectId, role) 已存在则覆盖；状态保持 DRAFT（双方均完成时 → SUBMITTED）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ContributionView saveSelf(Long projectId, ContributionSaveReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        Project project = requireProject(projectId);
        requireG5Stage(project);
        validateRole(req.role());
        validateScoreRange("立项主导", req.dimInitiation());
        validateScoreRange("差异化创新", req.dimInnovation());
        validateScoreRange("上市节奏", req.dimLaunch());
        validateScoreRange("市场结果", req.dimMarketResult());
        validateScoreRange("协同领导力", req.dimLeadership());
        assertRoleMatchesActor(actor, req.role());

        BigDecimal tier = computeTierCoefficient(req.dimInitiation(), req.dimInnovation(),
            req.dimLaunch(), req.dimMarketResult(), req.dimLeadership());

        Contribution existing = contributionMapper.selectOne(new LambdaQueryWrapper<Contribution>()
            .eq(Contribution::getProjectId, projectId)
            .eq(Contribution::getDelFlag, "0")
            .orderByDesc(Contribution::getId)
            .last("limit 1"));

        Contribution entity = existing != null ? existing : new Contribution();
        if (existing == null) {
            entity.setProjectId(projectId);
            entity.setStatus(Contribution.ST_DRAFT);
            entity.setMarketShare(new BigDecimal("0.55"));
            entity.setRdShare(new BigDecimal("0.45"));
            entity.setCreateTime(new Date());
        } else if (Contribution.ST_CONFIRMED.equals(existing.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "已确认的贡献度评定不可修改");
        }
        entity.setUpdateTime(new Date());
        entity.setUpdateBy(actor.id());

        if (Contribution.ROLE_MARKET.equals(req.role())) {
            entity.setMarketSelfInitiation(req.dimInitiation());
            entity.setMarketSelfInnovation(req.dimInnovation());
            entity.setMarketSelfLaunch(req.dimLaunch());
            entity.setMarketSelfMarketResult(req.dimMarketResult());
            entity.setMarketSelfLeadership(req.dimLeadership());
            entity.setMarketComment(req.comment());
        } else {
            entity.setRdSelfInitiation(req.dimInitiation());
            entity.setRdSelfInnovation(req.dimInnovation());
            entity.setRdSelfLaunch(req.dimLaunch());
            entity.setRdSelfMarketResult(req.dimMarketResult());
            entity.setRdSelfLeadership(req.dimLeadership());
            entity.setRdComment(req.comment());
        }
        entity.setTierCoefficient(tier);

        // 双方自评均完成 → SUBMITTED
        boolean bothDone = isFiveDimPresent(entity.getMarketSelfInitiation())
            && isFiveDimPresent(entity.getRdSelfInitiation());
        if (bothDone && Contribution.ST_DRAFT.equals(entity.getStatus())) {
            entity.setStatus(Contribution.ST_SUBMITTED);
            entity.setSubmittedAt(new Date());
        }

        if (entity.getId() == null) {
            contributionMapper.insert(entity);
        } else {
            contributionMapper.updateById(entity);
        }

        auditLogService.append(AuditLog.builder()
            .action("CONTRIBUTION_SAVE")
            .entityType("Contribution")
            .entityId(entity.getId())
            .operatorId(actor.id())
            .reason("{\"role\":\"" + req.role() + "\",\"tierCoefficient\":\""
                + tier.toPlainString() + "\",\"status\":\"" + entity.getStatus() + "\"}")
            .build());

        log.info("[{}] 保存贡献度自评 projectId={} role={} tier={} status={}",
            actor.id(), projectId, req.role(), tier.toPlainString(), entity.getStatus());
        return toView(entity);
    }

    /* ===========================================================
     *  调整市场 PM 比例（联动研发比例，AC-INC-25）
     * =========================================================== */

    /**
     * 调整市场 PM 比例：区间校验通过 → 落 marketShare，rdShare = 1.0 - market（AC-INC-25）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ContributionView adjustMarketShare(Long projectId, BigDecimal marketShare) {
        IpdActor actor = ipdPermission.requireInternal();
        Project project = requireProject(projectId);
        requireG5Stage(project);
        validateMarketShare(marketShare);

        Contribution entity = requireLatestContribution(projectId);
        if (Contribution.ST_CONFIRMED.equals(entity.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "已确认的贡献度比例不可调整");
        }
        BigDecimal oldShare = entity.getMarketShare();
        entity.setMarketShare(marketShare);
        entity.setRdShare(BigDecimal.ONE.subtract(marketShare));
        entity.setUpdateTime(new Date());
        entity.setUpdateBy(actor.id());
        contributionMapper.updateById(entity);

        auditLogService.append(AuditLog.builder()
            .action("CONTRIBUTION_ADJUST_SHARE")
            .entityType("Contribution")
            .entityId(entity.getId())
            .operatorId(actor.id())
            .reason("{\"oldShare\":\"" + oldShare + "\",\"newShare\":\""
                + marketShare.toPlainString() + "\"}")
            .build());

        log.info("[{}] 调整贡献度比例 projectId={} {} -> {}",
            actor.id(), projectId, oldShare, marketShare.toPlainString());
        return toView(entity);
    }

    /* ===========================================================
     *  产品组长确认（SUBMITTED → CONFIRMED）
     * =========================================================== */

    /**
     * 产品组长确认贡献度；幂等（已 CONFIRMED 不重复落审计）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ContributionView confirm(Long projectId, String decision, String opinion) {
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        Project project = requireProject(projectId);
        requireG5Stage(project);

        Contribution entity = requireLatestContribution(projectId);
        if (!Contribution.ST_SUBMITTED.equals(entity.getStatus())
            && !Contribution.ST_DRAFT.equals(entity.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "贡献度评定必须为 DRAFT/SUBMITTED 状态才能确认（当前=" + entity.getStatus() + "）");
        }

        if ("APPROVE".equals(decision)) {
            // 确认前再次校验市场比例
            if (entity.getMarketShare() == null) {
                throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_TIER_OUT_OF_RANGE,
                    "市场 PM 比例未填写");
            }
            validateMarketShare(entity.getMarketShare());
            entity.setStatus(Contribution.ST_CONFIRMED);
            entity.setLeaderId(actor.id());
            entity.setLeaderDecision("APPROVE");
            entity.setLeaderDecidedAt(new Date());
            entity.setLeaderOpinion(opinion);
        } else if ("REJECT".equals(decision)) {
            entity.setStatus(Contribution.ST_DRAFT);
            entity.setLeaderId(actor.id());
            entity.setLeaderDecision("REJECT");
            entity.setLeaderDecidedAt(new Date());
            entity.setLeaderOpinion(opinion);
        } else {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "leaderDecision 必须为 APPROVE 或 REJECT");
        }
        entity.setUpdateTime(new Date());
        entity.setUpdateBy(actor.id());
        contributionMapper.updateById(entity);

        auditLogService.append(AuditLog.builder()
            .action("CONTRIBUTION_CONFIRM")
            .entityType("Contribution")
            .entityId(entity.getId())
            .operatorId(actor.id())
            .reason("{\"decision\":\"" + decision + "\",\"marketShare\":\""
                + entity.getMarketShare().toPlainString() + "\",\"tierCoefficient\":\""
                + entity.getTierCoefficient().toPlainString() + "\"}")
            .build());

        log.info("[{}] 组长确认贡献度 projectId={} decision={} tier={}",
            actor.id(), projectId, decision, entity.getTierCoefficient().toPlainString());

        // BR-INC-09「归档版本可追溯」：APPROVE 确认时刻归档不可变快照（REJECT 不归档）
        if ("APPROVE".equals(decision)) {
            archiveConfirmed(entity, actor.id());
        }
        return toView(entity);
    }

    /**
     * 版本历史列表：该项目的历次确认归档快照（versionNo 降序，最新确认在前）。
     * <p>2026-09-08 前端契约对照轮补交：此前仅单行 contributions，无版本追溯。
     */
    @Transactional(readOnly = true)
    public List<ContributionVersionView> listVersions(Long projectId) {
        ipdPermission.requireInternal();
        requireProject(projectId);
        List<ContributionVersion> rows = versionMapper.selectList(
            Wrappers.<ContributionVersion>lambdaQuery()
                .eq(ContributionVersion::getProjectId, projectId)
                .orderByDesc(ContributionVersion::getVersionNo));
        return rows.stream().map(ContributionService::toVersionView).toList();
    }

    /**
     * 归档确认快照。versionNo = 同项目历史最大版次 + 1；
     * uk(project_id, version_no) 冲突时重算重试一次（同项目双组长并发的窄场景）。
     */
    private void archiveConfirmed(Contribution entity, Long operatorId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            ContributionVersion snapshot = ContributionVersion.builder()
                .sourceId(entity.getId())
                .projectId(entity.getProjectId())
                .versionNo(nextVersionNo(entity.getProjectId()))
                .status(Contribution.ST_CONFIRMED)
                .marketShare(entity.getMarketShare())
                .rdShare(entity.getRdShare())
                .marketSelfInitiation(entity.getMarketSelfInitiation())
                .marketSelfInnovation(entity.getMarketSelfInnovation())
                .marketSelfLaunch(entity.getMarketSelfLaunch())
                .marketSelfMarketResult(entity.getMarketSelfMarketResult())
                .marketSelfLeadership(entity.getMarketSelfLeadership())
                .rdSelfInitiation(entity.getRdSelfInitiation())
                .rdSelfInnovation(entity.getRdSelfInnovation())
                .rdSelfLaunch(entity.getRdSelfLaunch())
                .rdSelfMarketResult(entity.getRdSelfMarketResult())
                .rdSelfLeadership(entity.getRdSelfLeadership())
                .tierCoefficient(entity.getTierCoefficient())
                .marketComment(entity.getMarketComment())
                .rdComment(entity.getRdComment())
                .leaderId(entity.getLeaderId())
                .leaderDecision(entity.getLeaderDecision())
                .leaderDecidedAt(entity.getLeaderDecidedAt())
                .leaderOpinion(entity.getLeaderOpinion())
                .submittedAt(entity.getSubmittedAt())
                .archivedBy(operatorId)
                .archivedAt(new Date())
                .build();
            try {
                versionMapper.insert(snapshot);
                return;
            } catch (DuplicateKeyException duplicate) {
                if (attempt > 0) {
                    throw duplicate;
                }
            }
        }
    }

    private Integer nextVersionNo(Long projectId) {
        List<ContributionVersion> latest = versionMapper.selectList(
            Wrappers.<ContributionVersion>lambdaQuery()
                .eq(ContributionVersion::getProjectId, projectId)
                .orderByDesc(ContributionVersion::getVersionNo)
                .last("limit 1"));
        return latest.isEmpty() ? 1 : latest.get(0).getVersionNo() + 1;
    }

    /* ===========================================================
     *  内部辅助
     * =========================================================== */

    private Project requireProject(Long projectId) {
        if (projectId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "projectId 不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "项目不存在或已删除");
        }
        return project;
    }

    /**
     * G5 阶段门控：仅上市后 90 天复盘阶段开放（AC-INC-28）。
     * <p>判定规则：项目当前阶段 = G5；容许 LIFECYCLE 阶段（已上市）进入。
     * 简化：要求项目 status IN ("LIFECYCLE", "POST_LAUNCH")，否则拒。
     */
    private void requireG5Stage(Project project) {
        String status = project.getStatus();
        if (!"LIFECYCLE".equals(status) && !"POST_LAUNCH".equals(status)) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_NOT_G5_STAGE,
                "项目不在 G5 上市后 90 天复盘阶段（当前 status=" + status + "）");
        }
    }

    private Contribution requireLatestContribution(Long projectId) {
        Contribution entity = contributionMapper.selectOne(new LambdaQueryWrapper<Contribution>()
            .eq(Contribution::getProjectId, projectId)
            .eq(Contribution::getDelFlag, "0")
            .orderByDesc(Contribution::getId)
            .last("limit 1"));
        if (entity == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "项目尚未初始化贡献度评定");
        }
        return entity;
    }

    private static void validateRole(String role) {
        if (!PM_ROLES.contains(role)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "role 必须为 MARKET_PM 或 RD_PM（当前=" + role + "）");
        }
    }

    private static void validateScoreRange(String label, BigDecimal score) {
        if (score == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, label + "分数不能为空");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                label + "分数必须在 [0, 100] 区间（当前=" + score.toPlainString() + "）");
        }
    }

    /**
     * 校验 actor 与 role 一致：MARKET_PM 评市场维度，RD_PM 评研发维度。
     * <p>防止横向越权（AC-INC-28：仅双 PM 自评）。
     */
    private static void assertRoleMatchesActor(IpdActor actor, String role) {
        if (!actor.role().equals(role)) {
            throw new IpdBusinessException(ApiV1ErrorCode.CONTRIB_NOT_AUTHORIZED,
                "评定人角色与提交 role 不一致（actor=" + actor.role() + ", req=" + role + "）");
        }
    }

    private static boolean isFiveDimPresent(BigDecimal v) {
        return v != null;
    }

    private static ContributionView toView(Contribution c) {
        // weightsValid：五维度权重固定 25/25/20/20/10 = 100，落库即视为合法
        boolean weightsValid = c.getTierCoefficient() != null;
        return ContributionView.builder()
            .id(c.getId())
            .projectId(c.getProjectId())
            .status(c.getStatus())
            .marketShare(c.getMarketShare())
            .rdShare(c.getRdShare())
            .weightsValid(weightsValid)
            // 暴露给前端用于联动显示（合并双 PM 的最大维度以方便预览）
            .dimInitiation(maxOrNull(c.getMarketSelfInitiation(), c.getRdSelfInitiation()))
            .dimInnovation(maxOrNull(c.getMarketSelfInnovation(), c.getRdSelfInnovation()))
            .dimLaunch(maxOrNull(c.getMarketSelfLaunch(), c.getRdSelfLaunch()))
            .dimMarketResult(maxOrNull(c.getMarketSelfMarketResult(), c.getRdSelfMarketResult()))
            .dimLeadership(maxOrNull(c.getMarketSelfLeadership(), c.getRdSelfLeadership()))
            .tierCoefficient(c.getTierCoefficient())
            .marketComment(c.getMarketComment())
            .rdComment(c.getRdComment())
            .leaderId(c.getLeaderId())
            .leaderDecision(c.getLeaderDecision())
            .leaderDecidedAt(c.getLeaderDecidedAt())
            .leaderOpinion(c.getLeaderOpinion())
            .submittedAt(c.getSubmittedAt())
            .createTime(c.getCreateTime())
            .updateTime(c.getUpdateTime())
            .build();
    }

    private static ContributionVersionView toVersionView(ContributionVersion v) {
        return ContributionVersionView.builder()
            .id(v.getId())
            .projectId(v.getProjectId())
            .versionNo(v.getVersionNo())
            .status(v.getStatus())
            .marketShare(v.getMarketShare())
            .rdShare(v.getRdShare())
            .dimInitiation(maxOrNull(v.getMarketSelfInitiation(), v.getRdSelfInitiation()))
            .dimInnovation(maxOrNull(v.getMarketSelfInnovation(), v.getRdSelfInnovation()))
            .dimLaunch(maxOrNull(v.getMarketSelfLaunch(), v.getRdSelfLaunch()))
            .dimMarketResult(maxOrNull(v.getMarketSelfMarketResult(), v.getRdSelfMarketResult()))
            .dimLeadership(maxOrNull(v.getMarketSelfLeadership(), v.getRdSelfLeadership()))
            .tierCoefficient(v.getTierCoefficient())
            .marketComment(v.getMarketComment())
            .rdComment(v.getRdComment())
            .leaderId(v.getLeaderId())
            .leaderDecision(v.getLeaderDecision())
            .leaderDecidedAt(v.getLeaderDecidedAt())
            .leaderOpinion(v.getLeaderOpinion())
            .submittedAt(v.getSubmittedAt())
            .archivedBy(v.getArchivedBy())
            .archivedAt(v.getArchivedAt())
            .build();
    }

    private static BigDecimal maxOrNull(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    /* 用于暴露的五维度权重常量集合（测试/前端可读） */
    public static List<BigDecimal> defaultDimensionWeights() {
        return Arrays.asList(W_INITIATION, W_INNOVATION, W_LAUNCH, W_MARKET_RESULT, W_LEADERSHIP);
    }
}