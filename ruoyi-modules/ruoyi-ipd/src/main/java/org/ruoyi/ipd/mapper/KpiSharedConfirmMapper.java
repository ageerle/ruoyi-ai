package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.KpiSharedConfirm;

/**
 * 共担 KPI 双组长确认 Mapper（P3-1.2-BACKEND；table = kpi_shared_confirms）
 *
 * <p>复用 MyBatis-Plus {@link BaseMapperPlus} 提供默认 CRUD；
 * 查询组装走 Service 层 {@code LambdaQueryWrapper}，不在本接口额外声明。
 */
public interface KpiSharedConfirmMapper extends BaseMapperPlus<KpiSharedConfirm, KpiSharedConfirm> {
}
