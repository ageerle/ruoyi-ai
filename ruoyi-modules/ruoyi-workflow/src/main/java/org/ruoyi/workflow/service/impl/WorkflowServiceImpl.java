package org.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.dto.CompleteTaskDTO;
import org.ruoyi.common.core.domain.dto.StartProcessDTO;
import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.WorkflowService;
import org.ruoyi.common.core.utils.StringUtils;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.MessageTypeEnum;
import org.ruoyi.workflow.domain.FlowInstanceBizExt;
import org.ruoyi.workflow.domain.bo.CompleteTaskBo;
import org.ruoyi.workflow.domain.bo.StartProcessBo;
import org.ruoyi.workflow.service.IFlwDefinitionService;
import org.ruoyi.workflow.service.IFlwInstanceService;
import org.ruoyi.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService flwInstanceService;
    private final IFlwDefinitionService flwDefinitionService;
    private final IFlwTaskService flwTaskService;

    /**
     * 删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<Long> businessIds) {
        return flwInstanceService.deleteByBusinessIds(businessIds);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        FlowInstance flowInstance = flwInstanceService.selectByTaskId(taskId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     */
    @Override
    public String getBusinessStatus(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variables  流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variables) {
        flwInstanceService.setVariable(instanceId, variables);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        return flwInstanceService.instanceVariable(instanceId);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getId() : null;
    }

    /**
     * 新增租户流程定义
     *
     * @param tenantId 租户id
     */
    @Override
    public void syncDef(String tenantId) {
        flwDefinitionService.syncDef(tenantId);
    }

    /**
     * 启动流程
     *
     * @param startProcess 参数
     */
    @Override
    public StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess) {
        return flwTaskService.startWorkFlow(BeanUtil.toBean(startProcess, StartProcessBo.class));
    }

    /**
     * 办理任务
     * 系统后台发起审批 无用户信息 需要忽略权限
     * completeTask.getVariables().put("ignore", true);
     *
     * @param completeTask 参数
     */
    @Override
    public boolean completeTask(CompleteTaskDTO completeTask) {
        return flwTaskService.completeTask(BeanUtil.toBean(completeTask, CompleteTaskBo.class));
    }

    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     */
    @Override
    public boolean completeTask(Long taskId, String message) {
        CompleteTaskBo completeTask = new CompleteTaskBo();
        completeTask.setTaskId(taskId);
        completeTask.setMessage(message);
        // 忽略权限(系统后台发起审批 无用户信息 需要忽略权限)
        completeTask.getVariables().put("ignore", true);
        return flwTaskService.completeTask(completeTask);
    }

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startCompleteTask(StartProcessDTO startProcess) {
        try {
            StartProcessBo processBo = new StartProcessBo();
            processBo.setBusinessId(startProcess.getBusinessId());
            processBo.setFlowCode(startProcess.getFlowCode());
            processBo.setVariables(startProcess.getVariables());
            processBo.setHandler(startProcess.getHandler());
            processBo.setBizExt(BeanUtil.toBean(startProcess.getBizExt(), FlowInstanceBizExt.class));

            StartProcessReturnDTO result = flwTaskService.startWorkFlow(processBo);
            CompleteTaskBo taskBo = new CompleteTaskBo();
            taskBo.setTaskId(result.getTaskId());
            taskBo.setMessageType(Collections.singletonList(MessageTypeEnum.SYSTEM_MESSAGE.getCode()));
            taskBo.setVariables(startProcess.getVariables());
            taskBo.setHandler(startProcess.getHandler());

            boolean flag = flwTaskService.completeTask(taskBo);
            if (!flag) {
                throw new ServiceException("流程发起异常");
            }
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
