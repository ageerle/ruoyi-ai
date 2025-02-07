package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.PaymentOrder;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.PaymentOrdersBo;
import org.ruoyi.system.domain.bo.SysPackagePlanBo;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.request.OrderRequest;
import org.ruoyi.system.domain.vo.PaymentOrdersVo;
import org.ruoyi.system.domain.vo.SysPackagePlanVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.PaymentOrdersMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.IPaymentOrdersService;
import org.ruoyi.system.service.ISysPackagePlanService;
import org.ruoyi.system.service.ISysUserService;
import org.ruoyi.system.util.OrderNumberGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付订单Service业务层处理
 *
 * @author Lion Li
 * @date 2024-04-16
 */
@RequiredArgsConstructor
@Service
public class PaymentOrdersServiceImpl implements IPaymentOrdersService {

    private final PaymentOrdersMapper baseMapper;

    private final SysUserMapper sysUserMapper;

    private final ISysUserService userService;

    private final ISysPackagePlanService planService;

    /**
     * 查询支付订单
     */
    @Override
    public PaymentOrdersVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 创建支付订单
     */
    @Override
    public PaymentOrdersBo createPayOrder(OrderRequest orderRequest) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        PaymentOrdersBo paymentOrder = new PaymentOrdersBo();
        paymentOrder.setOrderName(orderRequest.getName());
        paymentOrder.setAmount(new BigDecimal(orderRequest.getMoney()));
        paymentOrder.setOrderNo(OrderNumberGenerator.generate());
        paymentOrder.setUserId(loginUser.getUserId());
        // TODO 支付状态默认待支付 - 添加枚举
        paymentOrder.setPaymentStatus("1");
        // 保存支付订单
        insertByBo(paymentOrder);
        return paymentOrder;
    }

    /**
     * 修改订单状态为已支付
     *
     */
    @Override
    public void updatePayOrder(OrderRequest orderRequest) {
        PaymentOrdersBo paymentOrdersBo = new PaymentOrdersBo();
        paymentOrdersBo.setOrderNo(orderRequest.getOrderNo());
        List<PaymentOrdersVo> paymentOrdersList = queryList(paymentOrdersBo);
        if (CollectionUtil.isEmpty(paymentOrdersList)){
            throw new BaseException("订单不存在！");
        }
        // 根据价格查询套餐
        SysPackagePlanBo sysPackagePlanBo = new SysPackagePlanBo();
        sysPackagePlanBo.setPrice(new BigDecimal(orderRequest.getMoney()));
        SysPackagePlanVo sysPackagePlanVo = planService.queryList(sysPackagePlanBo).get(0);


        // 订单状态修改为已支付
        PaymentOrdersVo paymentOrdersVo = paymentOrdersList.get(0);
        // 1 未支付 2未支付
        paymentOrdersVo.setPaymentStatus("2");
        paymentOrdersVo.setPaymentMethod(orderRequest.getPayType());
        BeanUtil.copyProperties(paymentOrdersVo,paymentOrdersBo);
        updateByBo(paymentOrdersBo);
        // 用户充值费用
        double money = paymentOrdersBo.getAmount().doubleValue();
        SysUserVo sysUserVo = userService.selectUserById(paymentOrdersVo.getUserId());
        sysUserVo.setUserBalance(sysUserVo.getUserBalance() + money);
        SysUserBo sysUserBo = new SysUserBo();
        BeanUtil.copyProperties(sysUserVo,sysUserBo);
        // 设置为付费用户
        sysUserBo.setUserGrade("1");
        sysUserBo.setUserPlan(sysPackagePlanVo.getId().toString());
        userService.updateUser(sysUserBo);
    }

    /**
     * 查询支付订单列表
     */
    @Override
    public TableDataInfo<PaymentOrdersVo> queryPageList(PaymentOrdersBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PaymentOrder> lqw = buildQueryWrapper(bo);
        Page<PaymentOrdersVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(CollectionUtil.isEmpty(result.getRecords())){
            return TableDataInfo.build(result);
        }
        // 获取所有userId
        List<Long> userIds = result.getRecords().stream()
            .map(PaymentOrdersVo::getUserId)
            .collect(Collectors.toList());
        // 一次性查询所有userName
        Map<Long, String> userIdToUserNameMap = getUserNamesByUserIds(userIds);
        // 设置userName
        result.getRecords().forEach(paymentOrderVo -> {
            paymentOrderVo.setUserName(userIdToUserNameMap.get(paymentOrderVo.getUserId()));
        });
        return TableDataInfo.build(result);
    }
    private Map<Long, String> getUserNamesByUserIds(List<Long> userIds) {
        // 实现批量查询userName的逻辑，例如通过sysUserMapper查询sys_user表
        List<SysUser> sysUsers = sysUserMapper.selectBatchIds(userIds);
        return sysUsers.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getUserName));
    }

    /**
     * 查询支付订单列表
     */
    @Override
    public List<PaymentOrdersVo> queryList(PaymentOrdersBo bo) {
        LambdaQueryWrapper<PaymentOrder> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PaymentOrder> buildQueryWrapper(PaymentOrdersBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PaymentOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), PaymentOrder::getOrderNo, bo.getOrderNo());
        lqw.like(StringUtils.isNotBlank(bo.getOrderName()), PaymentOrder::getOrderName, bo.getOrderName());
        lqw.eq(bo.getAmount() != null, PaymentOrder::getAmount, bo.getAmount());
        lqw.eq(StringUtils.isNotBlank(bo.getPaymentStatus()), PaymentOrder::getPaymentStatus, bo.getPaymentStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPaymentMethod()), PaymentOrder::getPaymentMethod, bo.getPaymentMethod());
        lqw.eq(bo.getUserId() != null, PaymentOrder::getUserId, bo.getUserId());
        return lqw;
    }

    /**
     * 新增支付订单
     */
    @Override
    public Boolean insertByBo(PaymentOrdersBo bo) {
        PaymentOrder add = MapstructUtils.convert(bo, PaymentOrder.class);
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
        PaymentOrder update = MapstructUtils.convert(bo, PaymentOrder.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PaymentOrder entity){
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
