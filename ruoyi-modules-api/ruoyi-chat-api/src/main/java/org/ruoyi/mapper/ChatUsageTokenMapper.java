package org.ruoyi.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.ChatUsageToken;
import org.ruoyi.domain.vo.ChatUsageTokenVo;

/**
 * 用户token使用详情Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface ChatUsageTokenMapper extends BaseMapperPlus<ChatUsageToken, ChatUsageTokenVo> {

}
