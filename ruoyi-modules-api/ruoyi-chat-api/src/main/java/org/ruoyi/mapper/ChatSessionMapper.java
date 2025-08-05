package org.ruoyi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.ChatSession;
import org.ruoyi.domain.vo.ChatSessionVo;

/**
 * 会话管理Mapper接口
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Mapper
public interface ChatSessionMapper extends BaseMapperPlus<ChatSession, ChatSessionVo> {

}
