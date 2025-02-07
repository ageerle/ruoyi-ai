package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.PaymentOrdersBo;
import org.ruoyi.system.domain.request.OrderRequest;
import org.ruoyi.system.domain.vo.PaymentOrdersVo;

import java.util.Collection;
import java.util.List;

/**
 * 支付订单Service接口
 *
 * @author Lion Li
 * @date 2024-04-16
 */
public interface IPaymentOrdersService {

    /**
     * 查询支付订单
     */
    PaymentOrdersVo queryById(Long id);


    /**
     * 创建支付订单
     */
    PaymentOrdersBo createPayOrder(OrderRequest orderRequest);

    /**
     * 修改订单状态为已支付
     *
     */
    void updatePayOrder(OrderRequest orderRequest);


    /**
     * 查询支付订单列表
     */
    TableDataInfo<PaymentOrdersVo> queryPageList(PaymentOrdersBo bo, PageQuery pageQuery);

    /**
     * 查询支付订单列表
     */
    List<PaymentOrdersVo> queryList(PaymentOrdersBo bo);

    /**
     * 新增支付订单
     */
    Boolean insertByBo(PaymentOrdersBo bo);

    /**
     * 修改支付订单
     */
    Boolean updateByBo(PaymentOrdersBo bo);

    /**
     * 校验并批量删除支付订单信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
