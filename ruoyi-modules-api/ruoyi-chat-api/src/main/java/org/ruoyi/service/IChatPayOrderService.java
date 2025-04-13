package org.ruoyi.service;

import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatPayOrderBo;
import org.ruoyi.domain.vo.ChatPayOrderVo;

import java.util.Collection;
import java.util.List;

/**
 * 支付订单Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatPayOrderService {

    /**
     * 查询支付订单
     */
    ChatPayOrderVo queryById(Long id);

    /**
     * 查询支付订单列表
     */
    TableDataInfo<ChatPayOrderVo> queryPageList(ChatPayOrderBo bo, PageQuery pageQuery);

    /**
     * 查询支付订单列表
     */
    List<ChatPayOrderVo> queryList(ChatPayOrderBo bo);

    /**
     * 新增支付订单
     */
    Boolean insertByBo(ChatPayOrderBo bo);

    /**
     * 修改支付订单
     */
    Boolean updateByBo(ChatPayOrderBo bo);

    /**
     * 校验并批量删除支付订单信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
