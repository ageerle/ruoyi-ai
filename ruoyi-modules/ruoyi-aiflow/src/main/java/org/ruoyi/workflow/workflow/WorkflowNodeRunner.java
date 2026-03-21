package org.ruoyi.workflow.workflow;

import org.ruoyi.workflow.entity.WorkflowNode;

import java.util.Map;

/**
 * 回调接口，负责执行业务节点并返回下游编排所需的元数据。
 */
@FunctionalInterface
public interface WorkflowNodeRunner {

    Map<String, Object> run(WorkflowNode node, WfNodeState nodeState);
}
