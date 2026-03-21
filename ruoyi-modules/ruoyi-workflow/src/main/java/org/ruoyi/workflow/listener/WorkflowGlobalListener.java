package org.ruoyi.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.enums.BusinessStatusEnum;
import org.ruoyi.common.core.service.UserService;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.common.enums.TaskStatusEnum;
import org.ruoyi.workflow.domain.bo.FlowCopyBo;
import org.ruoyi.workflow.domain.vo.NodeExtVo;
import org.ruoyi.workflow.handler.FlowProcessEventHandler;
import org.ruoyi.workflow.service.IFlwCommonService;
import org.ruoyi.workflow.service.IFlwInstanceService;
import org.ruoyi.workflow.service.IFlwNodeExtService;
import org.ruoyi.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全局任务办理监听
 *
 * @author may
 */
@ConditionalOnEnable
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowGlobalListener implements GlobalListener {

    private final IFlwTaskService flwTaskService;
    private final IFlwInstanceService flwInstanceService;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final IFlwCommonService flwCommonService;
    private final IFlwNodeExtService nodeExtService;
    private final UserService userService;

    /**
     * 创建监听器，任务创建时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void create(ListenerVariable listenerVariable) {

    }

    /**
     * 开始监听器，任务开始办理时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void start(ListenerVariable listenerVariable) {
        String ext = listenerVariable.getNode().getExt();
        if (StringUtils.isNotBlank(ext)) {
            Map<String, Object> variable = listenerVariable.getVariable();
            NodeExtVo nodeExt = nodeExtService.parseNodeExt(ext, variable);
            Set<String> copyList = nodeExt.getCopySettings();
            if (CollUtil.isNotEmpty(copyList)) {
                List<FlowCopyBo> list = StreamUtils.toList(copyList, x -> {
                    FlowCopyBo bo = new FlowCopyBo();
                    Long id = Convert.toLong(x);
                    bo.setUserId(id);
                    bo.setUserName(userService.selectUserNameById(id));
                    return bo;
                });
                variable.put(FlowConstant.FLOW_COPY_LIST, list);
            }
            if (CollUtil.isNotEmpty(nodeExt.getVariables())) {
                variable.putAll(nodeExt.getVariables());
            }
        }
    }

    /**
     * 分派监听器，动态修改代办任务信息
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void assignment(ListenerVariable listenerVariable) {
        Map<String, Object> variable = listenerVariable.getVariable();
        List<Task> nextTasks = listenerVariable.getNextTasks();
        FlowParams flowParams = listenerVariable.getFlowParams();
        Definition definition = listenerVariable.getDefinition();
        Instance instance = listenerVariable.getInstance();
        String applyNodeCode = flwCommonService.applyNodeCode(definition.getId());
        for (Task flowTask : nextTasks) {
            // 如果办理或者退回并行存在需要指定办理人，则直接覆盖办理人
            if (variable.containsKey(flowTask.getNodeCode()) && TaskStatusEnum.isPassOrBack(flowParams.getHisStatus())) {
                String userIds = variable.get(flowTask.getNodeCode()).toString();
                flowTask.setPermissionList(List.of(userIds.split(StringUtils.SEPARATOR)));
                variable.remove(flowTask.getNodeCode());
            }
            // 如果是申请节点，则把启动人添加到办理人
            if (flowTask.getNodeCode().equals(applyNodeCode)) {
                flowTask.setPermissionList(List.of(instance.getCreateBy()));
            }
        }
    }

    /**
     * 完成监听器，当前任务完成后执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void finish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        Task task = listenerVariable.getTask();
        List<Task> nextTasks = listenerVariable.getNextTasks();
        Map<String, Object> params = new HashMap<>();
        FlowParams flowParams = listenerVariable.getFlowParams();
        Map<String, Object> variable = new HashMap<>();
        if (ObjectUtil.isNotNull(flowParams)) {
            // 历史任务扩展(通常为附件)
            params.put("hisTaskExt", flowParams.getHisTaskExt());
            // 办理人
            params.put("handler", flowParams.getHandler());
            // 办理意见
            params.put("message", flowParams.getMessage());
            variable = flowParams.getVariable();
        }
        //申请人提交事件
        Boolean submit = MapUtil.getBool(variable, FlowConstant.SUBMIT);
        if (submit != null && submit) {
            flowProcessEventHandler.processHandler(definition.getFlowCode(), instance, instance.getFlowStatus(), variable, true);
        } else {
            // 判断流程状态（发布：撤销，退回，作废，终止，已完成事件）
            String status = determineFlowStatus(instance);
            if (StringUtils.isNotBlank(status)) {
                flowProcessEventHandler.processHandler(definition.getFlowCode(), instance, status, params, false);
            }
            if (!BusinessStatusEnum.initialState(instance.getFlowStatus())) {
                if (task != null && CollUtil.isNotEmpty(nextTasks) && nextTasks.size() == 1
                    && flwCommonService.applyNodeCode(definition.getId()).equals(nextTasks.get(0).getNodeCode())) {
                    // 如果为画线指定驳回 线条指定为驳回 驳回得节点为申请人节点 则修改流程状态为退回
                    flowProcessEventHandler.processHandler(definition.getFlowCode(), instance, BusinessStatusEnum.BACK.getStatus(), params, false);
                    // 修改流程实例状态
                    instance.setFlowStatus(BusinessStatusEnum.BACK.getStatus());
                    FlowEngine.insService().updateById(instance);
                }
            }
        }
        //发布任务事件
        if (CollUtil.isNotEmpty(nextTasks)) {
            for (Task nextTask : nextTasks) {
                flowProcessEventHandler.processTaskHandler(definition.getFlowCode(), instance, nextTask.getId(), params);
            }
        }
        if (ObjectUtil.isNull(flowParams)) {
            return;
        }
        // 只有办理或者退回的时候才执行消息通知和抄送
        if (!TaskStatusEnum.isPassOrBack(flowParams.getHisStatus())) {
            return;
        }
        if (ObjectUtil.isNull(variable)) {
            return;
        }

        if (variable.containsKey(FlowConstant.FLOW_COPY_LIST)) {
            List<FlowCopyBo> flowCopyList = MapUtil.get(variable, FlowConstant.FLOW_COPY_LIST, new TypeReference<>() {
            });
            // 添加抄送人
            flwTaskService.setCopy(task, flowCopyList);
        }
        if (variable.containsKey(FlowConstant.MESSAGE_TYPE)) {
            List<String> messageType = MapUtil.get(variable, FlowConstant.MESSAGE_TYPE, new TypeReference<>() {
            });
            String notice = MapUtil.getStr(variable, FlowConstant.MESSAGE_NOTICE);
            flwCommonService.sendMessage(definition.getFlowName(), instance.getId(), messageType, notice);
        }
        FlowEngine.insService().removeVariables(instance.getId(),
            FlowConstant.FLOW_COPY_LIST,
            FlowConstant.MESSAGE_TYPE,
            FlowConstant.MESSAGE_NOTICE,
            FlowConstant.SUBMIT
        );
    }

    /**
     * 根据流程实例确定最终状态
     *
     * @param instance 流程实例
     * @return 流程最终状态
     */
    private String determineFlowStatus(Instance instance) {
        String flowStatus = instance.getFlowStatus();
        if (StringUtils.isNotBlank(flowStatus) && BusinessStatusEnum.initialState(flowStatus)) {
            log.info("流程实例当前状态: {}", flowStatus);
            return flowStatus;
        } else {
            Long instanceId = instance.getId();
            if (flwTaskService.isTaskEnd(instanceId)) {
                String status = BusinessStatusEnum.FINISH.getStatus();
                // 更新流程状态为已完成
                flwInstanceService.updateStatus(instanceId, status);
                log.info("流程已结束，状态更新为: {}", status);
                return status;
            }
            return null;
        }
    }

}
