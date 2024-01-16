package com.xmzs.system.service;

import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.system.domain.bo.PaymentOrdersBo;
import com.xmzs.system.domain.vo.PaymentOrdersVo;

import java.util.Collection;
import java.util.List;

/**
 * 支付订单Service接口
 *
 * @author Lion Li
 * @date 2023-12-29
 */
public interface IPaymentOrdersService {

    /**
     * 查询支付订单
     */
    PaymentOrdersVo queryById(Long id);

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
