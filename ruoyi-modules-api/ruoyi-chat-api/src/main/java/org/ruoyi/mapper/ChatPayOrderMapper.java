package org.ruoyi.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.ChatPayOrder;
import org.ruoyi.domain.vo.ChatPayOrderVo;

/**
 * 支付订单Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface ChatPayOrderMapper extends BaseMapperPlus<ChatPayOrder, ChatPayOrderVo> {

}
