package org.ruoyi.workflow.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.ruoyi.workflow.domain.bo.FlowCancelBo;
import org.ruoyi.workflow.domain.bo.FlowInstanceBo;
import org.ruoyi.workflow.domain.bo.FlowInvalidBo;
import org.ruoyi.workflow.domain.bo.FlowVariableBo;
import org.ruoyi.workflow.domain.vo.FlowInstanceVo;

import java.util.List;
import java.util.Map;

/**
 * 流程实例 服务层
 *
 * @author may
 */
public interface IFlwInstanceService {

    /**
     * 分页查询正在运行的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery);

    /**
     * 分页查询已结束的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery);

    /**
     * 根据业务id查询流程实例详细信息
     *
     * @param businessId 业务id
     * @return 结果
     */
    FlowInstanceVo queryByBusinessId(Long businessId);

    /**
     * 按照业务id查询流程实例
     *
     * @param businessId 业务id
     * @return 结果
     */
    FlowInstance selectInstByBusinessId(String businessId);

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceId 实例id
     * @return 结果
     */
    FlowInstance selectInstById(Long instanceId);

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceIds 实例id
     * @return 结果
     */
    List<FlowInstance> selectInstListByIdList(List<Long> instanceIds);

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    boolean deleteByBusinessIds(List<Long> businessIds);

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     * @return 结果
     */
    boolean deleteByInstanceIds(List<Long> instanceIds);

    /**
     * 按照实例id删除已完成得流程实例
     *
     * @param instanceIds 删除的实例id
     * @return 删除结果
     */
    boolean deleteHisByInstanceIds(List<Long> instanceIds);

    /**
     * 撤销流程
     *
     * @param bo 参数
     * @return 结果
     */
    boolean cancelProcessApply(FlowCancelBo bo);

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 流程实例
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectCurrentInstanceList(FlowInstanceBo instanceBo, PageQuery pageQuery);

    /**
     * 获取流程图,流程记录
     *
     * @param businessId 业务id
     * @return 结果
     */
    Map<String, Object> flowHisTaskList(String businessId);

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    void updateStatus(Long instanceId, String status);

    /**
     * 获取流程变量
     *
     * @param instanceId 实例id
     * @return 结果
     */
    Map<String, Object> instanceVariable(Long instanceId);

    /**
     * 更新流程变量
     *
     * @param bo 参数
     * @return 结果
     */
    boolean updateVariable(FlowVariableBo bo);

    /**
     * 设置流程变量
     *
     * @param instanceId 实例id
     * @param variable   流程变量
     */
    void setVariable(Long instanceId, Map<String, Object> variable);

    /**
     * 按任务id查询实例
     *
     * @param taskId 任务id
     * @return 结果
     */
    FlowInstance selectByTaskId(Long taskId);

    /**
     * 作废流程
     *
     * @param bo 流程实例
     * @return 结果
     */
    boolean processInvalid(FlowInvalidBo bo);
}
