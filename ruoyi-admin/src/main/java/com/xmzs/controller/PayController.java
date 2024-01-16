package com.xmzs.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.xmzs.common.config.PayConfig;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.oss.core.OssClient;
import com.xmzs.common.oss.entity.UploadResult;
import com.xmzs.common.oss.factory.OssFactory;
import com.xmzs.common.response.PayResponse;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.common.service.PayService;
import com.xmzs.common.utils.MD5Util;
import com.xmzs.system.domain.bo.PaymentOrdersBo;
import com.xmzs.system.domain.bo.SysUserBo;
import com.xmzs.system.domain.request.OrderRequest;
import com.xmzs.system.domain.vo.PaymentOrdersVo;
import com.xmzs.system.domain.vo.SysUserVo;
import com.xmzs.system.service.IPaymentOrdersService;
import com.xmzs.system.service.ISysUserService;
import com.xmzs.system.util.OrderNumberGenerator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pay")
@Slf4j
public class PayController {

    private final PayService payService;

    private final ISysUserService userService;

    private final IPaymentOrdersService paymentOrdersService;

    /**
     * 获取支付二维码
     *
     * @Date 2023/7/3
     * @param response
     * @return void
     **/
    @PostMapping("/payUrl")
    public R<PaymentOrdersVo> payUrl(HttpServletResponse response, @RequestBody OrderRequest orderRequest) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        // 创建订单
        PaymentOrdersBo paymentOrders = new PaymentOrdersBo();
        paymentOrders.setOrderName(orderRequest.getName());
        paymentOrders.setAmount(new BigDecimal(orderRequest.getMoney()));
        String orderNo = OrderNumberGenerator.generate();
        paymentOrders.setOrderNo(orderNo);
        paymentOrders.setUserId(loginUser.getUserId());
        // TODO 支付状态默认待支付 - 添加枚举
        paymentOrders.setPaymentStatus("1");
        paymentOrdersService.insertByBo(paymentOrders);
        String payUrl = payService.getPayUrl(orderNo, orderRequest.getName(), Double.parseDouble(orderRequest.getMoney()), "192.168.1.6");
        byte[] bytes = QrCodeUtil.generatePng(payUrl, 300, 300);
        OssClient storage = OssFactory.instance();
        UploadResult upload=storage.upload(bytes, storage.getPath("qrCode",".png"), "image/png");
        PaymentOrdersVo paymentOrdersVo = new PaymentOrdersVo();
        BeanUtil.copyProperties(paymentOrders,paymentOrdersVo);
        paymentOrdersVo.setUrl(upload.getUrl());
        return R.ok(paymentOrdersVo);
    }


    /**
     * 跳转通知地址
     *
     * @Date 2023/7/3
     * @param
     * @return void
     **/
    @PostMapping("/notifyUrl")
    public void notifyUrl() {
        log.info("notifyUrl===========");
    }

    /**
     * 获取订单信息
     *
     */
    @PostMapping("/orderInfo")
    public R<PaymentOrdersVo> orderInfo(@RequestBody  OrderRequest orderRequest) {
        if(StringUtils.isEmpty(orderRequest.getOrderNo())){
            throw new BaseException("订单号不能为空！");
        }
        PaymentOrdersBo paymentOrdersBo = new PaymentOrdersBo();
        paymentOrdersBo.setOrderNo(orderRequest.getOrderNo());
        List<PaymentOrdersVo> paymentOrdersList = paymentOrdersService.queryList(paymentOrdersBo);
        if (CollectionUtil.isEmpty(paymentOrdersList)){
            throw new BaseException("订单不存在！");
        }
        PaymentOrdersVo paymentOrdersVo = paymentOrdersList.get(0);
        return R.ok(paymentOrdersVo);
    }

    /**
     * 跳转通知地址
     *
     * @Date 2023/7/3
     * @param payResponse
     * @return void
     **/
    @GetMapping("/returnUrl")
    public String returnUrl(PayResponse payResponse) {
        // 校验签名
        String mdString = "money=" + payResponse.getMoney() + "&name=" + payResponse.getName() +
            "&out_trade_no=" + payResponse.getOut_trade_no() + "&pid=" + PayConfig.pid +
            "&trade_no=" + payResponse.getTrade_no() + "&trade_status=" + payResponse.getTrade_status() +
            "&type=" + payResponse.getType() +  PayConfig.key;
        String sign = MD5Util.GetMD5Code(mdString);
        if(!sign.equals(payResponse.getSign())){
            throw new BaseException("校验签名失败！");
        }
        double money = Double.parseDouble(payResponse.getMoney());
        log.info("支付订单号{}",payResponse);
        PaymentOrdersBo paymentOrdersBo = new PaymentOrdersBo();
        paymentOrdersBo.setOrderNo(payResponse.getOut_trade_no());
        List<PaymentOrdersVo> paymentOrdersList = paymentOrdersService.queryList(paymentOrdersBo);
        if (CollectionUtil.isEmpty(paymentOrdersList)){
            throw new BaseException("订单不存在！");
        }
        // 订单状态修改为已支付
        PaymentOrdersVo paymentOrdersVo = paymentOrdersList.get(0);
        paymentOrdersVo.setPaymentStatus("2");
        paymentOrdersVo.setPaymentMethod(payResponse.getType());
        BeanUtil.copyProperties(paymentOrdersVo,paymentOrdersBo);
        paymentOrdersService.updateByBo(paymentOrdersBo);
        SysUserVo sysUserVo = userService.selectUserById(paymentOrdersVo.getUserId());
        sysUserVo.setUserBalance(sysUserVo.getUserBalance()+money);
        SysUserBo sysUserBo = new SysUserBo();
        BeanUtil.copyProperties(sysUserVo,sysUserBo);
        // 设置为付费用户
        sysUserBo.setUserGrade("1");
        userService.updateUser(sysUserBo);
        return "success";
    }

}

