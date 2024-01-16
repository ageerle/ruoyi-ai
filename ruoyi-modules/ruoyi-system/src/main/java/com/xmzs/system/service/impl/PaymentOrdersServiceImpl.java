package com.xmzs.system.service.impl;

import com.xmzs.common.core.utils.MapstructUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.xmzs.system.domain.bo.PaymentOrdersBo;
import com.xmzs.system.domain.vo.PaymentOrdersVo;
import com.xmzs.system.domain.PaymentOrders;
import com.xmzs.system.mapper.PaymentOrdersMapper;
import com.xmzs.system.service.IPaymentOrdersService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 支付订单Service业务层处理
 *
 * @author Lion Li
 * @date 2023-12-29
 */
@RequiredArgsConstructor
@Service
public class PaymentOrdersServiceImpl implements IPaymentOrdersService {

    private final PaymentOrdersMapper baseMapper;

    /**
     * 查询支付订单
     */
    @Override
    public PaymentOrdersVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询支付订单列表
     */
    @Override
    public TableDataInfo<PaymentOrdersVo> queryPageList(PaymentOrdersBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PaymentOrders> lqw = buildQueryWrapper(bo);
        Page<PaymentOrdersVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询支付订单列表
     */
    @Override
    public List<PaymentOrdersVo> queryList(PaymentOrdersBo bo) {
        LambdaQueryWrapper<PaymentOrders> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PaymentOrders> buildQueryWrapper(PaymentOrdersBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PaymentOrders> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), PaymentOrders::getOrderNo, bo.getOrderNo());
        lqw.like(StringUtils.isNotBlank(bo.getOrderName()), PaymentOrders::getOrderName, bo.getOrderName());
        lqw.eq(bo.getAmount() != null, PaymentOrders::getAmount, bo.getAmount());
        lqw.eq(StringUtils.isNotBlank(bo.getPaymentStatus()), PaymentOrders::getPaymentStatus, bo.getPaymentStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPaymentMethod()), PaymentOrders::getPaymentMethod, bo.getPaymentMethod());
        lqw.eq(bo.getUserId() != null, PaymentOrders::getUserId, bo.getUserId());
        return lqw;
    }

    /**
     * 新增支付订单
     */
    @Override
    public Boolean insertByBo(PaymentOrdersBo bo) {
        PaymentOrders add = MapstructUtils.convert(bo, PaymentOrders.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改支付订单
     */
    @Override
    public Boolean updateByBo(PaymentOrdersBo bo) {
        PaymentOrders update = MapstructUtils.convert(bo, PaymentOrders.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PaymentOrders entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除支付订单
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
