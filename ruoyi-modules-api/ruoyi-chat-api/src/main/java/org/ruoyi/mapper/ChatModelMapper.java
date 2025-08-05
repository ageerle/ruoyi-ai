package org.ruoyi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.ChatModel;
import org.ruoyi.domain.vo.ChatModelVo;

/**
 * 聊天模型Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface ChatModelMapper extends BaseMapperPlus<ChatModel, ChatModelVo> {

}
