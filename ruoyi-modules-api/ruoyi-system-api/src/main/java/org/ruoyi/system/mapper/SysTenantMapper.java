package org.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.SysTenant;
import org.ruoyi.system.domain.vo.SysTenantVo;

/**
 * 租户Mapper接口
 *
 * @author Michelle.Chung
 */
@Mapper
public interface SysTenantMapper extends BaseMapperPlus<SysTenant, SysTenantVo> {

}
