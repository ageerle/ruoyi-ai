package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.dto.ComputeBonusPoolReq;
import org.ruoyi.ipd.dto.AutoComputeBonusPoolReq;
import org.ruoyi.ipd.dto.DistributeBonusPoolReq;
import org.ruoyi.ipd.dto.FreezeBonusPoolReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.BonusPoolService;
import org.ruoyi.ipd.vo.BonusPoolVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 奖金池核算 Controller（P3-4.4，前端 P0-10.34 激励管理-奖金池核算）
 *
 * <p>5 端点：
 * <ul>
 *   <li>{@code POST /api/v1/bonus-pool/compute} — 计算并落库（DRAFT），权限 ipd:bonus-pool:compute</li>
 *   <li>{@code POST /api/v1/bonus-pool/{id}/freeze} — 冻结/确认（DRAFT→CONFIRMED），权限 ipd:bonus-pool:freeze</li>
 *   <li>{@code POST /api/v1/bonus-pool/{id}/distribute} — 分配（DRAFT/CONFIRMED→DISTRIBUTED），权限 ipd:bonus-pool:distribute</li>
 *   <li>{@code GET  /api/v1/bonus-pool/{id}} — 详情，权限 ipd:bonus-pool:query</li>
 *   <li>{@code GET  /api/v1/bonus-pool/list} — 项目奖金池列表，权限 ipd:bonus-pool:query</li>
 * </ul>
 *
 * <p>权限梯度：
 * <ul>
 *   <li>compute / query / list → {@code ipd:bonus-pool:compute|query}（MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN）</li>
 *   <li>freeze / distribute → {@code ipd:bonus-pool:freeze|distribute}（GROUP_LEADER / SUPER_ADMIN 涉钱审批）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/bonus-pool")
@RequiredArgsConstructor
@Validated
public class BonusPoolController {

    private final IpdPermission ipdPermission;
    private final BonusPoolService bonusPoolService;

    /**
     * P3-4.4 §2.1：奖金池核算（计算并落库 DRAFT）。
     *
     * <p>公式：{@code finalPool = actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient}
     * （ZK-IPD §三.2.1 + §三.2.5 完整公式）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_COMPUTE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/compute")
    public ApiV1Response<BonusPoolVO> compute(@Valid @RequestBody ComputeBonusPoolReq req) {
        // 兕底与注解同严：注解限超管，方法内不再放宽（第六批判例，防 Catalog 漂移时资金操作失防）
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(BonusPoolVO.from(bonusPoolService.compute(
            req.projectId(),
            req.actualReceipts(),
            req.achievementRate(),
            req.personalCoefficient(),
            req.poolRate(),
            actor
        )));
    }

    /**
     * [SEC-FIX-HIGH-5.2] 自动推导 personalCoefficient 的奖金池计算——
     * 从 kpi_records.comprehensive_score 推导个人绩效系数（不接 personalCoefficient 入参）。
     *
     * <p>[SEC-FIX-HIGH-5.2-FOLLOWUP] 注解层守卫补齐：与 {@link #compute} 同注解
     * {@code OPERATION_BONUS_POOL_COMPUTE}（Catalog GROUP_LEADER / SUPER_ADMIN），
     * 防 Service 层兜底被绕过时注解防御失守。
     * @param req 含 projectId/actualReceipts/achievementRate/poolRate/period（period 必填 YYYY-MM）
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_COMPUTE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/auto-compute")
    public ApiV1Response<BonusPoolVO> autoCompute(@Valid @RequestBody AutoComputeBonusPoolReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        // ① 推导 personalCoefficient
        java.math.BigDecimal personal = bonusPoolService.resolvePersonalCoefficient(
            req.projectId(), req.period());
        // ② 走标准 compute 链路（保持审计/状态机一致）
        return ApiV1Response.ok(BonusPoolVO.from(bonusPoolService.compute(
            req.projectId(),
            req.actualReceipts(),
            req.achievementRate(),
            personal,
            req.poolRate(),
            actor
        )));
    }

    /**
     * P3-4.4 §2.2：冻结/确认奖金池（DRAFT → CONFIRMED）。
     * 幂等：同状态再调不写第二条审计。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_FREEZE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/freeze")
    public ApiV1Response<BonusPoolVO> freeze(@PathVariable Long id,
                                           @RequestBody(required = false) FreezeBonusPoolReq req) {
        // 兕底与注解同严（第六批判例）
        IpdActor actor = ipdPermission.requireAdmin();
        String reason = (req == null) ? null : req.reason();
        return ApiV1Response.ok(BonusPoolVO.from(bonusPoolService.freeze(id, reason, actor)));
    }

    /**
     * P3-4.4 §2.3：分配奖金池（DRAFT/CONFIRMED → DISTRIBUTED）。
     * 比例校验走 §三.2.4 calculateDistribution（区段 + 总和双重护栏）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_DISTRIBUTE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/distribute")
    public ApiV1Response<BonusPoolVO> distribute(@PathVariable Long id,
                                               @Valid @RequestBody DistributeBonusPoolReq req) {
        // 兕底与注解同严（第六批判例）
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(BonusPoolVO.from(bonusPoolService.distribute(
            id, req.marketShare(), req.rdShare(), actor)));
    }

    /**
     * P3-4.4 §2.4：奖金池详情。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{id}")
    public ApiV1Response<BonusPoolVO> getById(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(BonusPoolVO.from(bonusPoolService.getById(id)));
    }

    /**
     * P3-4.4 §2.5：按项目查询奖金池列表（按 calculatedAt 倒序）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/list")
    public ApiV1Response<List<BonusPoolVO>> list(@RequestParam Long projectId) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bonusPoolService.listByProject(projectId).stream().map(BonusPoolVO::from).toList());
    }
}
