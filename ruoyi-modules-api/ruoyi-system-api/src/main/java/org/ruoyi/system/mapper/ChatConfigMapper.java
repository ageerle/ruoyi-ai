package org.ruoyi.system.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.ChatConfig;
import org.ruoyi.system.domain.vo.ChatConfigVo;

/**
 * 配置信息Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface ChatConfigMapper extends BaseMapperPlus<ChatConfig, ChatConfigVo> {

}
