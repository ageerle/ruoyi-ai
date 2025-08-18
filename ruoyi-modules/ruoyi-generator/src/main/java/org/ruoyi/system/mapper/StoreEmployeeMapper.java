package org.ruoyi.system.mapper;

import org.ruoyi.system.domain.StoreEmployee;
import org.ruoyi.system.domain.vo.StoreEmployeeVo;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工分配Mapper接口
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@Mapper
public interface StoreEmployeeMapper extends BaseMapperPlus<StoreEmployee, StoreEmployeeVo> {

}
