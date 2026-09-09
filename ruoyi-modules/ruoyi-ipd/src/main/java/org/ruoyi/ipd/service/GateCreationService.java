package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;

/**
 * P1 / §5.3 HIGH-1.1：G3 评审自动创建入口（独立端点 POST /api/v1/projects/{id}/gates?gateCode=）。
 *
 * <p>约束（按 governance-1 §5.6）：
  - 同一项目同一 gateCode 每 14 天最多创建 1 次（防刷创建 / 审计噪声）
  - gateCode ∈ {G1, G2, G3, G4, G5}（ZK-IPD §三.1）
  - 项目状态非 ARCHIVED 才允许创建
  - 写 GATE_AUTO_CREATE 审计行
 *
 * <p>使用方式：调用方（前端/超管）传入 gateCode，服务创建 GateReview 主记录（gateId 自动生成；
 * 后续 GateReviewService.sign 进入双签流）。
 */
@Service
@RequiredArgsConstructor
public class GateCreationService {

    /** §5.3 / ZK-IPD §三.1：合法 gateCode 枚举 */
    public static final Set<String> ALLOWED_GATE_CODES = Set.of("G1", "G2", "G3", "G4", "G5");

    /** §5.3：每 14 天最多创建 1 次（毫秒） */
    public static final long CREATE_COOLDOWN_MS = 14L * 24 * 3600 * 1000;

    private final GateReviewMapper gateReviewMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    /** 可注入时钟（仿 stateMachineGuard 模式；测试固定时刻消除真实时钟摇摆，生产零影响）。 */
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();
    public void setClock(java.time.Clock clock) {
        this.clock = (clock == null) ? java.time.Clock.systemDefaultZone() : clock;
    }
    private Date now() { return Date.from(clock.instant()); }

    @Transactional(rollbackFor = Exception.class)
    public GateReview autoCreateGate(Long projectId, String gateCode, Long operatorId) {
        if (projectId == null) {
            throw new ServiceException("项目 ID 不能为空");
        }
        if (gateCode == null || !ALLOWED_GATE_CODES.contains(gateCode)) {
            throw new ServiceException("gateCode 非法（允许 G1/G2/G3/G4/G5）: " + gateCode);
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        if ("ARCHIVED".equals(project.getStatus()) || "SUSPENDED".equals(project.getStatus())) {
            throw new ServiceException("归档/暂停项目不可创建 Gate 评审");
        }
        // 冷却窗口：同项目同 gateCode 最近 14 天内不可重复创建
        Date cutoff = new Date(now().getTime() - CREATE_COOLDOWN_MS);
        Long recent = gateReviewMapper.selectCount(new LambdaQueryWrapper<GateReview>()
            .eq(GateReview::getProjectId, projectId)
            .eq(GateReview::getGateCode, gateCode)
            .ge(GateReview::getCreateTime, cutoff));
        if (recent != null && recent > 0) {
            throw new ServiceException(gateCode + " 评审每 14 天最多自动创建 1 次（最近 14 天已有 " + recent + " 条）");
        }
        GateReview review = GateReview.builder()
            .projectId(projectId)
            .gateCode(gateCode)
            .reviewerType("MARKET_PM") // 默认发起方；具体双签由 GateReviewService.sign 后续覆盖
            .decision("PENDING")
            .round(1)
            .signDueAt(new Date(now().getTime() + 3L * 24 * 3600 * 1000)) // 默认 3 天签署期
            .signExtensionCount(0)
            .delFlag("0")
            .build();
        review.setCreateTime(now());
        gateReviewMapper.insert(review);
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .action("GATE_AUTO_CREATE")
            .entityType("gate_reviews")
            .entityId(review.getId())
            .reason("project:" + projectId + " gateCode:" + gateCode)
            .afterData("{\"projectId\":" + projectId + ",\"gateCode\":\"" + gateCode + "\",\"round\":1}")
            .createTime(now())
            .build());
        return review;
    }
}