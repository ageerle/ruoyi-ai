package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.KpiRecord;

/**
 * KPI 记录 Mapper（P3-1.1/1.2/1.3 通用；table = kpi_records）
 *
 * <p>复用 MyBatis-Plus {@link BaseMapperPlus} 提供默认 CRUD；
 * 复杂聚合查询走 Service 层 {@code LambdaQueryWrapper} 组装，不在本接口额外声明。
 */
public interface KpiRecordMapper extends BaseMapperPlus<KpiRecord, KpiRecord> {
}
