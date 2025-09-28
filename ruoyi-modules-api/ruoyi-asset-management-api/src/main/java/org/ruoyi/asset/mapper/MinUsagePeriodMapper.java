package org.ruoyi.asset.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.asset.domain.MinUsagePeriod;
import org.ruoyi.asset.domain.vo.MinUsagePeriodVo;

/**
 * 最低使用年限表 数据层
 *
 * @author cass
 * @date 2025-09-24
 */
@Mapper
public interface MinUsagePeriodMapper extends BaseMapperPlus<MinUsagePeriod, MinUsagePeriodVo> {

}
