package org.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.workflow.base.ThreadContext;
import org.ruoyi.workflow.dto.workflow.WfRuntimeNodeDto;
import org.ruoyi.workflow.entity.User;
import org.ruoyi.workflow.entity.WorkflowRuntimeNode;
import org.ruoyi.workflow.mapper.WorkflowRuntimeNodeMapper;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.util.MPPageUtil;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class WorkflowRuntimeNodeService extends ServiceImpl<WorkflowRuntimeNodeMapper, WorkflowRuntimeNode> {


    public List<WfRuntimeNodeDto> listByWfRuntimeId(long runtimeId) {
        List<WorkflowRuntimeNode> workflowNodeList = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(!ThreadContext.getCurrentUser().getIsAdmin(), WorkflowRuntimeNode::getUserId, ThreadContext.getCurrentUser().getId())
                .eq(WorkflowRuntimeNode::getWorkflowRuntimeId, runtimeId)
                .eq(WorkflowRuntimeNode::getIsDeleted, false)
                .orderByAsc(WorkflowRuntimeNode::getId)
                .list();
        List<WfRuntimeNodeDto> result = MPPageUtil.convertToList(workflowNodeList, WfRuntimeNodeDto.class);
        for (WfRuntimeNodeDto dto : result) {
            fillInputOutput(dto);
        }
        return result;
    }

    public WfRuntimeNodeDto createByState(User user, long wfNodeId, long wfRuntimeId, WfNodeState state) {
        WorkflowRuntimeNode runtimeNode = new WorkflowRuntimeNode();
        runtimeNode.setUuid(state.getUuid());
        runtimeNode.setWorkflowRuntimeId(wfRuntimeId);
        runtimeNode.setStatus(state.getProcessStatus());
        runtimeNode.setUserId(user.getId());
        runtimeNode.setNodeId(wfNodeId);
        baseMapper.insert(runtimeNode);
        runtimeNode = baseMapper.selectById(runtimeNode.getId());

        WfRuntimeNodeDto result = new WfRuntimeNodeDto();
        BeanUtils.copyProperties(runtimeNode, result);
        fillInputOutput(result);
        return result;
    }

    public void updateInput(Long id, WfNodeState state) {
        if (CollectionUtils.isEmpty(state.getInputs())) {
            log.warn("没有输入数据,id:{}", id);
            return;
        }
        WorkflowRuntimeNode node = baseMapper.selectById(id);
        if (null == node) {
            log.error("节点实例不存在,id:{}", id);
            return;
        }
        WorkflowRuntimeNode updateOne = new WorkflowRuntimeNode();
        updateOne.setId(id);
        ObjectNode ob = JsonUtil.createObjectNode();
        state.getInputs().forEach(data -> ob.set(data.getName(), JsonUtil.classToJsonNode(data.getContent())));
        updateOne.setInput(JsonUtil.toJson(ob));
        updateOne.setStatus(state.getProcessStatus());
        updateOne.setStatusRemark(state.getProcessStatusRemark());
        baseMapper.updateById(updateOne);
    }

    public void updateOutput(Long id, WfNodeState state) {
        WorkflowRuntimeNode node = baseMapper.selectById(id);
        if (null == node) {
            log.error("节点实例不存在,id:{}", id);
            return;
        }
        WorkflowRuntimeNode updateOne = new WorkflowRuntimeNode();
        updateOne.setId(id);
        if (!CollectionUtils.isEmpty(state.getOutputs())) {
            ObjectNode ob = JsonUtil.createObjectNode();
            state.getOutputs().forEach(data -> ob.set(data.getName(), JsonUtil.classToJsonNode(data.getContent())));
            updateOne.setOutput(JsonUtil.toJson(ob));
        }
        updateOne.setStatus(state.getProcessStatus());
        updateOne.setStatusRemark(state.getProcessStatusRemark());
        baseMapper.updateById(updateOne);
    }

    private void fillInputOutput(WfRuntimeNodeDto dto) {
        if (null == dto.getInput()) {
            dto.setInput(JsonUtil.createObjectNode());
        }
        if (null == dto.getOutput()) {
            dto.setOutput(JsonUtil.createObjectNode());
        }
    }

}
