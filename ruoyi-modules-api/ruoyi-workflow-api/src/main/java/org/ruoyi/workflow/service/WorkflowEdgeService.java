package org.ruoyi.workflow.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.dto.workflow.WfEdgeReq;
import org.ruoyi.workflow.entity.WorkflowEdge;
import org.ruoyi.workflow.enums.ErrorEnum;
import org.ruoyi.workflow.mapper.WorkflowEdgeMapper;
import org.ruoyi.workflow.util.MPPageUtil;
import org.ruoyi.workflow.util.UuidUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WorkflowEdgeService extends ServiceImpl<WorkflowEdgeMapper, WorkflowEdge> {

    @Lazy
    @Resource
    private WorkflowEdgeService self;

    public List<WfEdgeReq> listDtoByWfId(long workflowId) {
        List<WorkflowEdge> edges = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowEdge::getWorkflowId, workflowId)
                .eq(WorkflowEdge::getIsDeleted, false)
                .list();
        return MPPageUtil.convertToList(edges, WfEdgeReq.class);
    }

    @Transactional
    public void createOrUpdateEdges(Long workflowId, List<WfEdgeReq> edges) {
        List<String> uuidList = new ArrayList<>();
        for (WfEdgeReq edge : edges) {
            WorkflowEdge newOne = new WorkflowEdge();
            BeanUtils.copyProperties(edge, newOne);
            newOne.setWorkflowId(workflowId);

            WorkflowEdge old = self.getByUuid(edge.getUuid());
            if (null != old) {
                log.info("更新边,id:{},uuid:{},source:{},sourceHandle:{},target:{}",
                        edge.getId(), edge.getUuid(), edge.getSourceNodeUuid(), edge.getSourceHandle(), edge.getTargetNodeUuid());
                newOne.setId(old.getId());
            } else {
                newOne.setId(null);
                log.info("新增边,uuid:{},source:{},sourceHandle:{},target:{}",
                        edge.getUuid(), edge.getSourceNodeUuid(), edge.getSourceHandle(), edge.getTargetNodeUuid());
            }
            uuidList.add(edge.getUuid());
            self.saveOrUpdate(newOne);
        }
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(WorkflowEdge::getWorkflowId, workflowId)
                .notIn(CollUtil.isNotEmpty(uuidList), WorkflowEdge::getUuid, uuidList)
                .set(WorkflowEdge::getIsDeleted, true)
                .update();
    }

    public List<WorkflowEdge> listByWorkflowId(Long workflowId) {
        return ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowEdge::getWorkflowId, workflowId)
                .eq(WorkflowEdge::getIsDeleted, false)
                .list();
    }

    public List<WorkflowEdge> copyByWorkflowId(long workflowId, long targetWorkflow) {
        List<WorkflowEdge> result = new ArrayList<>();
        self.listByWorkflowId(workflowId).forEach(edge -> {
            result.add(self.copyEdge(targetWorkflow, edge));
        });
        return result;
    }

    public WorkflowEdge copyEdge(long targetWorkflow, WorkflowEdge sourceEdge) {
        WorkflowEdge newEdge = new WorkflowEdge();
        BeanUtils.copyProperties(sourceEdge, newEdge, "id", "uuid", "createTime", "updateTime");
        newEdge.setUuid(UuidUtil.createShort());
        newEdge.setWorkflowId(targetWorkflow);
        baseMapper.insert(newEdge);
        return getById(newEdge.getId());
    }

    @Transactional
    public void deleteEdges(Long workflowId, List<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) {
            return;
        }
        for (String uuid : uuids) {
            WorkflowEdge old = self.getByUuid(uuid);
            if (null != old && !old.getWorkflowId().equals(workflowId)) {
                log.error("该边不属于指定的工作流,删除失败,workflowId:{},node workflowId:{}", workflowId, workflowId);
                throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
            }
            ChainWrappers.lambdaUpdateChain(baseMapper)
                    .eq(WorkflowEdge::getWorkflowId, workflowId)
                    .eq(WorkflowEdge::getUuid, uuid)
                    .set(WorkflowEdge::getIsDeleted, true)
                    .update();
        }
    }

    public WorkflowEdge getByUuid(String uuid) {
        return ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowEdge::getUuid, uuid)
                .eq(WorkflowEdge::getIsDeleted, false)
                .last("limit 1")
                .one();
    }
}
