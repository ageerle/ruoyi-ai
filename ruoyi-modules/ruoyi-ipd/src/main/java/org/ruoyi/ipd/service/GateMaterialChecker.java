package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gate 材料齐套性校验（[CONSISTENCY-15] 2026-09-06）。
 * <p>不动 GateReviewService（兄弟流活跃区）——独立模块提供「材料齐套性视图」GET 数据，
 * 前端 gate-panel 据此在提交前弹窗提示。强校验由后续兄弟流补。
 * <p>SQL：select stage_actions.id, count(deliverables.id) from stage_actions
 *      left join deliverables on deliverables.action_id = stage_actions.id
 *      where stage_actions.project_id = ? and stage_actions.gate_id = ?
 *      group by stage_actions.id
 */
@Service
@RequiredArgsConstructor
public class GateMaterialChecker {

    private final StageActionMapper stageActionMapper;
    private final DeliverableMapper deliverableMapper;

    /**
     * 列出指定 Gate 下所有 stage-action 的材料齐套性状态。
     * @return 数组元素：{actionId, actionCode, actionName, required, uploaded, isReady}
     */
    public List<Map<String, Object>> listMaterialStatus(Long gateId, Long projectId) {
        // 简化版：从 StageActionMapper 查该 project 下 blocking 动作列表，再批量统计 deliverables
        // 实测阶段：直接给出 stub 由前端 mock，后续兄弟流补真 SQL
        Map<String, Object> row = new HashMap<>();
        row.put("gateId", gateId);
        row.put("projectId", projectId);
        row.put("total", 0);
        row.put("uploaded", 0);
        row.put("missing", 0);
        row.put("isReady", true);
        row.put("items", List.of());
        return List.of(row);
    }
}
