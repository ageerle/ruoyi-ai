package org.ruoyi.ipd.service.executor;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * Gate 软删除执行器（entityType=gates，P0-6.2 / AC-DEL-02）。
 * <p>幂等：已软删或不存在时不重复写。实体带 {@code @TableLogic}，
 * 已删行 {@code selectById} 返回 null，与 NOOP 语义一致。
 */
@Component
@RequiredArgsConstructor
public class GateSoftDeleteExecutor implements SoftDeleteExecutor<Gate> {

    public static final String ENTITY_TYPE = "gates";

    private final GateMapper gateMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<Gate> entityClass() {
        return Gate.class;
    }

    /**
     * 软删 Gate 实例（del_flag=1）。
     *
     * @param id Gate 主键
     */
    @Override
    public void softDelete(Long id) {
        Gate gate = gateMapper.selectById(id);
        if (gate == null || "1".equals(gate.getDelFlag())) {
            return;
        }
        gate.setDelFlag("1");
        int rows = gateMapper.updateById(gate);
        if (rows != 1) {
            throw new ServiceException("Gate 软删除未更新唯一记录: id=" + id);
        }
    }

    /**
     * 判断 Gate 是否已软删或不存在。
     *
     * @param id Gate 主键
     * @return true 表示应记 DELETE_NOOP
     */
    @Override
    public boolean isDeleted(Long id) {
        Gate gate = gateMapper.selectById(id);
        return gate == null || "1".equals(gate.getDelFlag());
    }
}
