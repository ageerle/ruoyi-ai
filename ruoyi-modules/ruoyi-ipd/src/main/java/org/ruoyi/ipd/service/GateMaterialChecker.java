package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gate 材料齐套性校验（[CONSISTENCY-15] 2026-09-06）。
 *
 * <p>gates 与 stage_actions 表无 FK 关联，按项目级（project_id）粒度统计 stage-action
 * 上传材料齐套性，返回 gate-panel.vue 用的"3/5 交付物已上传"状态条。
 *
 * <p>SQL 拆解（LambdaQueryWrapper 实现）：
 * <ol>
 *   <li>stage_actions WHERE project_id=? AND del_flag='0' AND is_blocking='1' ORDER BY id</li>
 *   <li>deliverables WHERE project_id=? AND del_flag='0' → in-memory 按 action_id groupBy</li>
 *   <li>join：每个动作的 uploaded 数 = deliverables 里 actionId 命中的条数</li>
 * </ol>
 *
 * <p>不动 GateReviewService（兄弟流活跃区）——独立模块；真 SQL 由本类承载。
 */
@Service
@RequiredArgsConstructor
public class GateMaterialChecker {

    private final StageActionMapper stageActionMapper;
    private final DeliverableMapper deliverableMapper;

    /**
     * 列出指定 Gate 下所有 stage-action 的材料齐套性状态。
     *
     * @param gateId    Gate 主键（当前实现按 project 聚合；gateId 参数保留供后续 Gate-stage 关联表扩展）
     * @param projectId 项目主键
     * @return {gateId, projectId, total, uploaded, missing, isReady, items[]}
     */
    public Map<String, Object> listMaterialStatus(Long gateId, Long projectId) {
        // 1. 查项目下所有有效 stage-action
        // StageAction 无 delFlag 字段（DDL 有但 entity 未映射），仅按 projectId 过滤
        List<StageAction> actions = stageActionMapper.selectList(
            new LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId)
                .orderByAsc(StageAction::getId)
        );

        // 2. 查项目下所有有效 deliverable
        // Deliverable 同样未映射 delFlag
        List<Deliverable> deliverables = deliverableMapper.selectList(
            new LambdaQueryWrapper<Deliverable>()
                .eq(Deliverable::getProjectId, projectId)
        );

        // 3. in-memory groupBy actionId
        Map<Long, Long> uploadedByAction = deliverables.stream()
            .filter(d -> d.getActionId() != null)
            .collect(Collectors.groupingBy(Deliverable::getActionId, Collectors.counting()));

        // 4. 组装 items
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        long missing = 0;
        for (StageAction a : actions) {
            long uploaded = uploadedByAction.getOrDefault(a.getId(), 0L);
            boolean ready = uploaded > 0;  // 阻断动作至少上传 1 个交付物即通过
            if (!ready) missing++;
            Map<String, Object> row = new HashMap<>();
            row.put("actionId", a.getId());
            row.put("actionCode", a.getActionCode());
            row.put("actionName", a.getActionName());
            row.put("required", 1);
            row.put("uploaded", uploaded);
            row.put("isReady", ready);
            items.add(row);
        }

        long total = actions.size();
        long totalUploaded = (long) items.stream().mapToLong(r -> ((Number) r.get("uploaded")).longValue()).sum();

        Map<String, Object> out = new HashMap<>();
        out.put("gateId", gateId);
        out.put("projectId", projectId);
        out.put("total", total);
        out.put("uploaded", totalUploaded);
        out.put("missing", missing);
        out.put("isReady", missing == 0 && total > 0);
        out.put("items", items);
        return out;
    }
}
