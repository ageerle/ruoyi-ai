package org.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.workflow.entity.WorkflowNode;

@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNode> {
    WorkflowNode getStartNode(long workflowId);
}
