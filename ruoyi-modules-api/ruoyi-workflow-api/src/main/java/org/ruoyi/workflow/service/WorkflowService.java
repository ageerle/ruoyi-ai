package org.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.base.ThreadContext;
import org.ruoyi.workflow.dto.workflow.WfEdgeReq;
import org.ruoyi.workflow.dto.workflow.WfNodeDto;
import org.ruoyi.workflow.dto.workflow.WorkflowResp;
import org.ruoyi.workflow.dto.workflow.WorkflowUpdateReq;
import org.ruoyi.workflow.entity.User;
import org.ruoyi.workflow.entity.Workflow;
import org.ruoyi.workflow.enums.ErrorEnum;
import org.ruoyi.workflow.mapper.WorkflowMapper;
import org.ruoyi.workflow.util.MPPageUtil;
import org.ruoyi.workflow.util.PrivilegeUtil;
import org.ruoyi.workflow.util.UuidUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WorkflowService extends ServiceImpl<WorkflowMapper, Workflow> {

    @Lazy
    @Resource
    private WorkflowService self;

    @Resource
    private WorkflowNodeService workflowNodeService;

    @Resource
    private WorkflowEdgeService workflowEdgeService;

    @Resource
    private WorkflowComponentService workflowComponentService;

    @Transactional
    public WorkflowResp add(String title, String remark, Boolean isPublic) {
        String uuid = UuidUtil.createShort();
        Workflow one = new Workflow();
        one.setUuid(uuid);
        one.setTitle(title);
        one.setUserId(ThreadContext.getCurrentUserId());
        one.setRemark(remark);
        one.setIsEnable(true);
        one.setIsPublic(isPublic);
        baseMapper.insert(one);

        workflowNodeService.createStartNode(one);
        return changeWorkflowToDTO(one);
    }

    public void setPublic(String wfUuid, Boolean isPublic) {
        Workflow workflow = PrivilegeUtil.checkAndGetByUuid(wfUuid, this.query(), ErrorEnum.A_WF_NOT_FOUND);
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(Workflow::getId, workflow.getId())
                .set(Workflow::getIsPublic, isPublic)
                .update();
    }

    public WorkflowResp updateBaseInfo(String wfUuid, String title, String remark, Boolean isPublic) {
        if (StringUtils.isAnyBlank(wfUuid, title)) {
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(Workflow::getUuid, wfUuid)
                .eq(!ThreadContext.getCurrentUser().getIsAdmin(), Workflow::getUserId, ThreadContext.getCurrentUserId())
                .set(Workflow::getTitle, title)
                .set(Workflow::getRemark, remark)
                .set(null != isPublic, Workflow::getIsPublic, isPublic)
                .update();
        Workflow workflow = getOrThrow(wfUuid);
        return changeWorkflowToDTO(workflow);
    }

    @Transactional
    public WorkflowResp update(WorkflowUpdateReq req) {
        Workflow workflow = PrivilegeUtil.checkAndGetByUuid(req.getUuid(), this.query(), ErrorEnum.A_WF_NOT_FOUND);
        long workflowId = workflow.getId();
        workflowNodeService.createOrUpdateNodes(workflowId, req.getNodes());
        workflowEdgeService.createOrUpdateEdges(workflowId, req.getEdges());
        Workflow workflow2 = getOrThrow(req.getUuid());
        return changeWorkflowToDTO(workflow2);
    }

    /**
     * 获取当前用户可访问的工作流详情
     *
     * @param uuid 工作流唯一标识
     * @return 工作流详情
     */
    public WorkflowResp getDetail(String uuid) {
        Workflow workflow = PrivilegeUtil.checkAndGetByUuid(uuid, this.query(), ErrorEnum.A_WF_NOT_FOUND);
        return changeWorkflowToDTO(workflow);
    }

    /**
     * 获取公开工作流详情
     *
     * @param uuid 工作流唯一标识
     * @return 工作流详情
     */
    public WorkflowResp getPublicDetail(String uuid) {
        Workflow workflow = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getUuid, uuid)
                .eq(Workflow::getIsDeleted, false)
                .eq(Workflow::getIsPublic, true)
                .eq(Workflow::getIsEnable, true)
                .last("limit 1")
                .one();
        if (null == workflow) {
            throw new BaseException(ErrorEnum.A_WF_NOT_FOUND.getInfo());
        }
        return changeWorkflowToDTO(workflow);
    }

    public Workflow getByUuid(String uuid) {
        return ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getUuid, uuid)
                .eq(Workflow::getIsDeleted, false)
                .last("limit 1")
                .one();
    }

    public Workflow getOrThrow(String uuid) {
        Workflow workflow = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getUuid, uuid)
                .eq(Workflow::getIsDeleted, false)
                .last("limit 1")
                .one();
        if (null == workflow) {
            throw new BaseException(ErrorEnum.A_WF_NOT_FOUND.getInfo());
        }
        return workflow;
    }

    public Page<WorkflowResp> search(String keyword, Boolean isPublic, Boolean isEnable, Integer currentPage, Integer pageSize) {
        User user = ThreadContext.getCurrentUser();
        Page<Workflow> page = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getIsDeleted, false)
                .eq(null != isPublic, Workflow::getIsPublic, isPublic)
                .eq(null != isEnable, Workflow::getIsEnable, isEnable)
                .like(StringUtils.isNotBlank(keyword), Workflow::getTitle, keyword)
                .eq(!user.getIsAdmin(), Workflow::getUserId, user.getId())
                .orderByDesc(Workflow::getUpdateTime)
                .page(new Page<>(currentPage, pageSize));
        Page<WorkflowResp> result = new Page<>();
        List<Long> userIds = new ArrayList<>();
        MPPageUtil.convertToPage(page, result, WorkflowResp.class, (source, target) -> {
            fillNodesAndEdges(target);
            userIds.add(source.getUserId());
            return target;
        });
//        fillUserInfos(userIds, result.getRecords());
        return result;
    }

    public Page<WorkflowResp> searchPublic(String keyword, Integer currentPage, Integer pageSize) {
        Page<Workflow> page = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getIsDeleted, false)
                .eq(Workflow::getIsPublic, true)
                .eq(Workflow::getIsEnable, true)
                .like(StringUtils.isNotBlank(keyword), Workflow::getTitle, keyword)
                .orderByDesc(Workflow::getUpdateTime)
                .page(new Page<>(currentPage, pageSize));
        Page<WorkflowResp> result = new Page<>();
        List<Long> userIds = new ArrayList<>();
        MPPageUtil.convertToPage(page, result, WorkflowResp.class, (source, target) -> {
            fillNodesAndEdges(target);
            userIds.add(source.getUserId());
            return target;
        });
        return result;
    }


    public Page<WorkflowResp> search(String keyword, Integer currentPage, Integer pageSize) {
        Page<Workflow> page = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(Workflow::getIsDeleted, false)
                .eq(Workflow::getIsEnable, true)
                .like(StringUtils.isNotBlank(keyword), Workflow::getTitle, keyword)
                .orderByDesc(Workflow::getUpdateTime)
                .page(new Page<>(currentPage, pageSize));
        Page<WorkflowResp> result = new Page<>();
        List<Long> userIds = new ArrayList<>();
        MPPageUtil.convertToPage(page, result, WorkflowResp.class, (source, target) -> {
            fillNodesAndEdges(target);
            userIds.add(source.getUserId());
            return target;
        });
        return result;
    }

    public void softDelete(String uuid) {
        ChainWrappers.lambdaUpdateChain(baseMapper).eq(Workflow::getUuid, uuid)
                .set(Workflow::getIsDeleted, true).update();
    }

    public void enable(String uuid, Boolean enable) {
        if (null == enable) {
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        Workflow workflow = PrivilegeUtil.checkAndGetByUuid(uuid, this.query(), ErrorEnum.A_WF_NOT_FOUND);
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(Workflow::getId, workflow.getId())
                .eq(!ThreadContext.getCurrentUser().getIsAdmin(), Workflow::getUserId, ThreadContext.getCurrentUserId())
                .set(Workflow::getIsEnable, enable)
                .update();
    }

    private WorkflowResp changeWorkflowToDTO(Workflow workflow) {
        WorkflowResp workflowResp = new WorkflowResp();
        BeanUtils.copyProperties(workflow, workflowResp);

        fillNodesAndEdges(workflowResp);
//        User user = userService.getById(workflow.getUserId());
//        if (null != user) {
//            workflowResp.setUserUuid(user.getUuid());
//            workflowResp.setUserName(user.getName());
//        }
        return workflowResp;
    }

    private void fillNodesAndEdges(WorkflowResp workflowResp) {
        List<WfNodeDto> nodes = workflowNodeService.listDtoByWfId(workflowResp.getId());
        workflowResp.setNodes(nodes);
        List<WfEdgeReq> edges = workflowEdgeService.listDtoByWfId(workflowResp.getId());
        workflowResp.setEdges(edges);
    }

}
