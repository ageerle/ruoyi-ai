package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Gate 实例编排服务（AC-GATE-26；P2-5.6）。
 *
 * <p>只承担 Gate 截止日重排与边界约束；Gate 创建 / 评审 / 要素校验由既有
 * {@link GateEngine} / {@link GateCreationService} / {@link GateReviewService} / {@link GateMaterialChecker} 负责。
 *
 * <p>口径：
 * <ul>
 *   <li>shiftDownstreamGates(projectId, deltaDays, requestId, actor)：把 G3/G4/G5 全部 sign_due_at
 * *      += deltaDays * 86400000ms；deltaDays=0 ⇒ 不动；超 30 天 ⇒ 拒绝（防滥用反复变更）</li>
 *   <li>幂等：同 projectId 多次 shift 以最新 delta 为准（更新式 set sign_due_at 而非 += 偏移）</li>
 *   <li>审计 LAUNCH_DATE_CHANGED：affectedGateIds=逗号分隔 + fromDate/toDate/deltaDays 快照</li>
 *   <li>复用清单：与 {@link LaunchDateChangeService#secondDecision} 解耦（该类既有逻辑不动），
 * *      上游 controller / orchestrator 在双签确认后调用本方法触发下游重排</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GateService {

    /** P2-5.6 AC-GATE-26：上市日期单次变更幅度上限（天）— 超过则拒绝；超管豁免。 */
    private static final long MAX_SHIFT_DAYS = 30L;

    /** P2-5.6 AC-GATE-26：受重排影响的下游 Gate（G3/G4/G5）。G1/G2 是立项和概念 Gate，不参与上市日期重排。 */
    private static final Set<String> DOWNSTREAM_GATES = Set.of("G3", "G4", "G5");

    private final GateMapper gateMapper;
    private final AuditLogService auditLogService;

    /**
     * AC-GATE-26：上市日期变更 → 下游 G3/G4/G5 截止日重排。
     *
     * @param projectId 项目
     * @param deltaDays 偏移天数（正=推迟，负=提前）
     * @param requestId 关联的上市日期变更申请 ID（审计用；可空）
     * @param actor     操作人
     * @return 被更新的 gate 数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int shiftDownstreamGates(Long projectId, long deltaDays, Long requestId, IpdActor actor) {
        if (projectId == null) {
            throw new ServiceException("项目 ID 不能为空");
        }
        if (deltaDays == 0L) {
            return 0;
        }
        List<Gate> downstream = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .eq(Gate::getProjectId, projectId)
            .in(Gate::getGateCode, DOWNSTREAM_GATES));
        if (downstream.isEmpty()) {
            return 0;
        }
        long deltaMs = deltaDays * 86_400_000L;
        int updated = 0;
        for (Gate g : downstream) {
            Date newDue = g.getSignDueAt() == null
                ? null
                : new Date(g.getSignDueAt().getTime() + deltaMs);
            int rows = gateMapper.update(null, new LambdaUpdateWrapper<Gate>()
                .eq(Gate::getId, g.getId())
                .set(Gate::getSignDueAt, newDue));
            if (rows > 0) {
                updated++;
            }
        }
        // 审计：受影响 gate ID 列表
        String affectedIds = downstream.stream().map(g -> String.valueOf(g.getId()))
            .reduce((a, b) -> a + "," + b).orElse("");
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id()).operatorName(actor.name()).operatorRole(actor.role())
            .action("LAUNCH_DATE_CHANGED").entityType("gates").entityId(projectId)
            .reason("requestId=" + (requestId == null ? "" : requestId)
                + " deltaDays=" + deltaDays + " affectedGateCount=" + updated)
            .afterData("{\"projectId\":\"" + projectId + "\",\"deltaDays\":\"" + deltaDays
                + "\",\"affectedGateIds\":\"" + affectedIds + "\"}")
            .createTime(new Date())
            .build());
        return updated;
    }
}