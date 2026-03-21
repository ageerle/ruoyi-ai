package org.ruoyi.workflow.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.workflow.domain.FlowInstanceBizExt;

import java.util.List;

/**
 * 流程实例业务扩展Mapper接口
 *
 * @author may
 * @date 2025-08-05
 */
public interface FlwInstanceBizExtMapper extends BaseMapperPlus<FlowInstanceBizExt, FlowInstanceBizExt> {

    /**
     * 根据 instanceId 保存或更新流程实例业务扩展
     *
     * @param entity 流程实例业务扩展实体
     * @return 操作是否成功
     */
    default int saveOrUpdateByInstanceId(FlowInstanceBizExt entity) {
        // 查询是否存在
        FlowInstanceBizExt exist = this.selectOne(new LambdaQueryWrapper<FlowInstanceBizExt>()
            .eq(FlowInstanceBizExt::getInstanceId, entity.getInstanceId()));

        if (ObjectUtil.isNotNull(exist)) {
            // 存在就带上主键更新
            entity.setId(exist.getId());
            return updateById(entity);
        } else {
            // 不存在就插入
            return insert(entity);
        }
    }

    /**
     * 按照流程实例ID删除单个流程实例业务扩展
     *
     * @param instanceId 流程实例ID
     * @return 删除的行数
     */
    default int deleteByInstId(Long instanceId) {
        return this.delete(new LambdaQueryWrapper<FlowInstanceBizExt>()
            .eq(FlowInstanceBizExt::getInstanceId, instanceId));
    }

    /**
     * 按照流程实例ID批量删除流程实例业务扩展
     *
     * @param instanceIds 流程实例ID列表
     * @return 删除的行数
     */
    default int deleteByInstIds(List<Long> instanceIds) {
        return this.delete(new LambdaQueryWrapper<FlowInstanceBizExt>()
            .in(FlowInstanceBizExt::getInstanceId, instanceIds));
    }

}
