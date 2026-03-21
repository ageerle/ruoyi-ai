package org.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.enums.BusinessStatusEnum;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.TaskStatusEnum;
import org.ruoyi.workflow.domain.bo.FlowCancelBo;
import org.ruoyi.workflow.domain.bo.FlowInstanceBo;
import org.ruoyi.workflow.domain.bo.FlowInvalidBo;
import org.ruoyi.workflow.domain.bo.FlowVariableBo;
import org.ruoyi.workflow.domain.vo.FlowHisTaskVo;
import org.ruoyi.workflow.domain.vo.FlowInstanceVo;
import org.ruoyi.workflow.handler.FlowProcessEventHandler;
import org.ruoyi.workflow.mapper.FlwCategoryMapper;
import org.ruoyi.workflow.mapper.FlwInstanceMapper;
import org.ruoyi.workflow.service.IFlwInstanceService;
import org.ruoyi.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwInstanceServiceImpl implements IFlwInstanceService {

    private final InsService insService;
    private final DefService defService;
    private final TaskService taskService;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final IFlwTaskService flwTaskService;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwCategoryMapper flwCategoryMapper;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi.flow_status", BusinessStatusEnum.runningStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi.flow_status", BusinessStatusEnum.finishStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 根据业务id查询流程实例详细信息
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public FlowInstanceVo queryByBusinessId(Long businessId) {
        FlowInstance instance = this.selectInstByBusinessId(Convert.toStr(businessId));
        FlowInstanceVo instanceVo = BeanUtil.toBean(instance, FlowInstanceVo.class);
        Definition definition = defService.getById(instanceVo.getDefinitionId());
        instanceVo.setFlowName(definition.getFlowName());
        instanceVo.setFlowCode(definition.getFlowCode());
        instanceVo.setVersion(definition.getVersion());
        instanceVo.setFormCustom(definition.getFormCustom());
        instanceVo.setFormPath(definition.getFormPath());
        instanceVo.setCategory(definition.getCategory());
        return instanceVo;
    }

    /**
     * 通用查询条件
     *
     * @param flowInstanceBo 查询条件
     * @return 查询条件构造方法
     */
    private QueryWrapper<FlowInstanceBo> buildQueryWrapper(FlowInstanceBo flowInstanceBo) {
        QueryWrapper<FlowInstanceBo> queryWrapper = Wrappers.query();
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getNodeName()), "fi.node_name", flowInstanceBo.getNodeName());
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getFlowName()), "fd.flow_name", flowInstanceBo.getFlowName());
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getFlowCode()), "fd.flow_code", flowInstanceBo.getFlowCode());
        if (StringUtils.isNotBlank(flowInstanceBo.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowInstanceBo.getCategory()));
            queryWrapper.in("fd.category", StreamUtils.toList(categoryIds, Convert::toStr));
        }
        queryWrapper.eq(StringUtils.isNotBlank(flowInstanceBo.getBusinessId()), "fi.business_id", flowInstanceBo.getBusinessId());
        queryWrapper.in(CollUtil.isNotEmpty(flowInstanceBo.getCreateByIds()), "fi.create_by", flowInstanceBo.getCreateByIds());
        queryWrapper.eq("fi.del_flag", "0");
        queryWrapper.orderByDesc("fi.create_time");
        return queryWrapper;
    }

    /**
     * 根据业务id查询流程实例
     *
     * @param businessId 业务id
     */
    @Override
    public FlowInstance selectInstByBusinessId(String businessId) {
        return flowInstanceMapper.selectOne(new LambdaQueryWrapper<FlowInstance>().eq(FlowInstance::getBusinessId, businessId));
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceId 实例id
     */
    @Override
    public FlowInstance selectInstById(Long instanceId) {
        return flowInstanceMapper.selectById(instanceId);
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    public List<FlowInstance> selectInstListByIdList(List<Long> instanceIds) {
        return flowInstanceMapper.selectByIds(instanceIds);
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBusinessIds(List<Long> businessIds) {
        List<FlowInstance> flowInstances = flowInstanceMapper.selectList(new LambdaQueryWrapper<FlowInstance>().in(FlowInstance::getBusinessId, StreamUtils.toList(businessIds, Convert::toStr)));
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        return insService.remove(StreamUtils.toList(flowInstances, FlowInstance::getId));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceIds(List<Long> instanceIds) {
        // 获取实例信息
        List<Instance> instances = insService.getByIds(instanceIds);
        if (CollUtil.isEmpty(instances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 获取定义信息
        Map<Long, Definition> definitionMap = StreamUtils.toMap(
            defService.getByIds(StreamUtils.toList(instances, Instance::getDefinitionId)),
            Definition::getId,
            Function.identity()
        );

        try {
            // 逐一触发删除事件
            instances.forEach(instance -> {
                Definition definition = definitionMap.get(instance.getDefinitionId());
                if (ObjectUtil.isNull(definition)) {
                    log.warn("实例 ID: {} 对应的流程定义信息未找到，跳过删除事件触发。", instance.getId());
                    return;
                }
                flowProcessEventHandler.processDeleteHandler(definition.getFlowCode(), instance.getBusinessId());
            });
            // 删除实例
            boolean remove = insService.remove(instanceIds);
            if (!remove) {
                log.warn("删除流程实例失败!");
                throw new ServiceException("删除流程实例失败");
            }
        } catch (Exception e) {
            log.warn("操作失败!{}", e.getMessage());
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 按照实例id删除已完成的流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteHisByInstanceIds(List<Long> instanceIds) {
        // 获取实例信息
        List<Instance> instances = insService.getByIds(instanceIds);
        if (CollUtil.isEmpty(instances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 获取定义信息
        Map<Long, Definition> definitionMap = StreamUtils.toMap(
            defService.getByIds(StreamUtils.toList(instances, Instance::getDefinitionId)),
            Definition::getId,
            Function.identity()
        );
        try {
            // 逐一触发删除事件
            instances.forEach(instance -> {
                Definition definition = definitionMap.get(instance.getDefinitionId());
                if (ObjectUtil.isNull(definition)) {
                    log.warn("实例 ID: {} 对应的流程定义信息未找到，跳过删除事件触发。", instance.getId());
                    return;
                }
                flowProcessEventHandler.processDeleteHandler(definition.getFlowCode(), instance.getBusinessId());
            });
            List<FlowTask> flowTaskList = flwTaskService.selectByInstIds(instanceIds);
            if (CollUtil.isNotEmpty(flowTaskList)) {
                FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(flowTaskList, FlowTask::getId));
            }
            FlowEngine.taskService().deleteByInsIds(instanceIds);
            FlowEngine.hisTaskService().deleteByInsIds(instanceIds);
            FlowEngine.insService().removeByIds(instanceIds);
        } catch (Exception e) {
            log.warn("操作失败!{}", e.getMessage());
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 撤销流程
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(FlowCancelBo bo) {
        try {
            Instance instance = selectInstByBusinessId(bo.getBusinessId());
            if (instance == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
            }
            Definition definition = defService.getById(instance.getDefinitionId());
            if (definition == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_DEF);
            }
            String message = bo.getMessage();
            String userIdStr = LoginHelper.getUserIdStr();
            BusinessStatusEnum.checkCancelStatus(instance.getFlowStatus());
            FlowParams flowParams = FlowParams.build()
                .message(message)
                .flowStatus(BusinessStatusEnum.CANCEL.getStatus())
                .hisStatus(BusinessStatusEnum.CANCEL.getStatus())
                .handler(userIdStr)
                .ignore(true);
            taskService.revoke(instance.getId(), flowParams);
        } catch (Exception e) {
            log.error("撤销失败: {}", e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 流程实例
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectCurrentInstanceList(FlowInstanceBo instanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(instanceBo);
        queryWrapper.eq("fi.create_by", LoginHelper.getUserIdStr());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 获取流程图,流程记录
     *
     * @param businessId 业务id
     */
    @Override
    public Map<String, Object> flowHisTaskList(String businessId) {
        FlowInstance flowInstance = this.selectInstByBusinessId(businessId);
        if (ObjectUtil.isNull(flowInstance)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Long instanceId = flowInstance.getId();

        // 先组装待审批任务（运行中的任务）
        List<FlowHisTaskVo> runningTaskVos = new ArrayList<>();
        List<FlowTask> runningTasks = flwTaskService.selectByInstId(instanceId);
        if (CollUtil.isNotEmpty(runningTasks)) {
            runningTaskVos = BeanUtil.copyToList(runningTasks, FlowHisTaskVo.class);

            List<User> associatedUsers = FlowEngine.userService()
                .getByAssociateds(StreamUtils.toList(runningTasks, FlowTask::getId));
            Map<Long, List<User>> taskUserMap = StreamUtils.groupByKey(associatedUsers, User::getAssociated);

            for (FlowHisTaskVo vo : runningTaskVos) {
                vo.setFlowStatus(TaskStatusEnum.WAITING.getStatus());
                vo.setUpdateTime(null);
                vo.setRunDuration(null);

                List<User> users = taskUserMap.get(vo.getId());
                if (CollUtil.isNotEmpty(users)) {
                    vo.setApprover(StreamUtils.join(users, User::getProcessedBy));
                }
            }
        }

        // 再组装历史任务（已处理任务）
        List<FlowHisTaskVo> hisTaskVos = new ArrayList<>();
        List<FlowHisTask> hisTasks = flowHisTaskMapper.selectList(
            new LambdaQueryWrapper<FlowHisTask>()
                .eq(FlowHisTask::getInstanceId, instanceId)
                .eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
                .orderByDesc(FlowHisTask::getUpdateTime)
        );
        if (CollUtil.isNotEmpty(hisTasks)) {
            hisTaskVos = BeanUtil.copyToList(hisTasks, FlowHisTaskVo.class);
        }

        // 结果列表，待审批任务在前，历史任务在后
        List<FlowHisTaskVo> combinedList = new ArrayList<>();
        combinedList.addAll(runningTaskVos);
        combinedList.addAll(hisTaskVos);

        return Map.of("list", combinedList, "instanceId", instanceId);
    }

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    @Override
    public void updateStatus(Long instanceId, String status) {
        LambdaUpdateWrapper<FlowInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(FlowInstance::getFlowStatus, status);
        wrapper.eq(FlowInstance::getId, instanceId);
        flowInstanceMapper.update(wrapper);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 实例id
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        FlowInstance flowInstance = flowInstanceMapper.selectById(instanceId);
        Map<String, Object> variableMap = Optional.ofNullable(flowInstance.getVariableMap()).orElse(Collections.emptyMap());
        List<Map<String, Object>> variableList = variableMap.entrySet().stream()
            .map(entry -> Map.of("key", entry.getKey(), "value", entry.getValue()))
            .toList();
        return Map.of("variableList", variableList, "variable", flowInstance.getVariable());
    }

    /**
     * 设置流程变量
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVariable(FlowVariableBo bo) {
        FlowInstance flowInstance = flowInstanceMapper.selectById(bo.getInstanceId());
        if (flowInstance == null) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        try {
            Map<String, Object> variableMap = new HashMap<>(Optional.ofNullable(flowInstance.getVariableMap()).orElse(Collections.emptyMap()));
            if (!variableMap.containsKey(bo.getKey())) {
                log.error("变量不存在: {}", bo.getKey());
                return false;
            }
            variableMap.put(bo.getKey(), bo.getValue());
            flowInstance.setVariable(FlowEngine.jsonConvert.objToStr(variableMap));
            flowInstanceMapper.updateById(flowInstance);
        } catch (Exception e) {
            log.error("设置流程变量失败: {}", e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 实例id
     * @param variable   流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variable) {
        Instance instance = insService.getById(instanceId);
        if (instance != null) {
            taskService.mergeVariable(instance, variable);
            insService.updateById(instance);
        }
    }

    /**
     * 按任务id查询实例
     *
     * @param taskId 任务id
     */
    @Override
    public FlowInstance selectByTaskId(Long taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            FlowHisTask flowHisTask = flwTaskService.selectHisTaskById(taskId);
            if (flowHisTask != null) {
                return this.selectInstById(flowHisTask.getInstanceId());
            }
        } else {
            return this.selectInstById(task.getInstanceId());
        }
        return null;
    }

    /**
     * 作废流程
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processInvalid(FlowInvalidBo bo) {
        try {
            Instance instance = insService.getById(bo.getId());
            if (instance != null) {
                BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
            }
            FlowParams flowParams = FlowParams.build()
                .message(bo.getComment())
                .flowStatus(BusinessStatusEnum.INVALID.getStatus())
                .hisStatus(TaskStatusEnum.INVALID.getStatus())
                .ignore(true);
            taskService.terminationByInsId(bo.getId(), flowParams);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }
}
