package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.BonusAllocation;

/**
 * 奖金分配台账 Mapper（AC-INC-35；P3-4.4；P-DATA-gap-1 接线）。
 *
 * <p>distribute 落台账批量写入口；注解 SQL 风格对齐 BonusPoolMapper（不依赖 XML）。
 */
public interface BonusAllocationMapper extends BaseMapperPlus<BonusAllocation, BonusAllocation> {
}
