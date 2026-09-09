package org.ruoyi.ipd.workbench;

import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 工作台任务聚合器（P1 方向 B，评审 §3.2；设计契约见 docs/ipd-系统说明/P1方向B聚合层设计-20260908.md）。
 *
 * 每个实现对应 spec 页03:165 权威枚举 17 类 taskType 之一，负责"按域投递任务卡"：
 * 责任匹配、状态机过滤、卡字段组装全部封装在实现内部，WorkbenchService.summary() 只做统一调度，
 * 新增一类任务 = 新增一个实现，不改 summary() 主流程。
 */
public interface WorkbenchAggregator {

    /** 该聚合器对应的 taskType 字符串（spec 页03:165 权威枚举 17 个之一）。 */
    String taskType();

    /**
     * 收集当前 actor 可见的该类任务，返回 task 卡列表。
     *
     * 卡字段约束（与 P0 stage_sign 同构，必填）：
     * id / projectId / projectName / projectCode / actionCode / title /
     * taskType（必须等于 {@link #taskType()}）/ status / priority / ownerRole /
     * dueDate / isBlocking / deepLink。
     * isBlocking 值域统一 '1'/'N'（前端以 isBlocking === '1' 判定阻断，P0 修过 'Y' 值域 bug 不得复发）。
     *
     * @param actor           当前登录人（SEC-API-01：仅从会话推导）
     * @param visibleProjects actor 可见项目 byId（避免各聚合器重复查可见范围）
     * @param now             统一时间戳（跨聚合器 overdue/priority 判定一致）
     */
    List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now);

    /**
     * 该聚合器域内的"已完成"计数（stats.completed 口径）；默认 0，
     * 仅 stage_sign 等有完成态语义的域覆盖（设计文档 §4 的最小扩展，避免 summary() 感知具体域）。
     */
    default int completedCount(IpdActor actor, Map<Long, Project> visibleProjects) {
        return 0;
    }
}
