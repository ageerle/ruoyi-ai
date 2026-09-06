package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.SystemConfigVersion;

/**
 * 参数版本链 Mapper（P0-3.3）。
 * 纪律：本 Mapper 不得出现 updateById(entity)/deleteById 业务调用——
 * 版本行唯一合法写路径为 insert；effective_to 闭合走 service 内受控 lambdaUpdate。
 */
public interface SystemConfigVersionMapper extends BaseMapperPlus<SystemConfigVersion, SystemConfigVersion> {
}
