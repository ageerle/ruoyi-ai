package org.ruoyi.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.ChatMessage;
import org.ruoyi.domain.vo.ChatMessageVo;

/**
 * 聊天消息Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface ChatMessageMapper extends BaseMapperPlus<ChatMessage, ChatMessageVo> {

}
