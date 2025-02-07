package org.ruoyi.system.controller.system;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.order.WxPayNativeOrderResult;
import com.github.binarywang.wxpay.bean.request.BaseWxPayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.config.PayConfig;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.oss.core.OssClient;
import org.ruoyi.common.oss.entity.UploadResult;
import org.ruoyi.common.oss.factory.OssFactory;
import org.ruoyi.common.response.PayResponse;
import org.ruoyi.common.service.PayService;
import org.ruoyi.common.utils.MD5Util;
import org.ruoyi.system.domain.bo.PaymentOrdersBo;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.request.OrderRequest;
import org.ruoyi.system.domain.vo.PaymentOrdersVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.service.IPaymentOrdersService;
import org.ruoyi.system.service.ISysUserService;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/pay")
@Slf4j
public class PayController {

    private final PayService payService;

    private final ISysUserService userService;

    private final IPaymentOrdersService paymentOrdersService;

    private final PayConfig payConfig;

    private final WxPayService wxService;

    private final ConfigService configService;

    /**
     * 获取支付二维码
     *
     * @Date 2023/7/3
     * @return void
     **/
    @PostMapping("/payUrl")
    public R<PaymentOrdersVo> payUrl(@RequestBody OrderRequest orderRequest) {
        PaymentOrdersBo payOrder = paymentOrdersService.createPayOrder(orderRequest);
        PaymentOrdersVo paymentOrdersVo = new PaymentOrdersVo();
        if(!Boolean.parseBoolean(getKey("enabled"))){
            String payUrl = payService.getPayUrl(payOrder.getOrderNo(), orderRequest.getName(), Double.parseDouble(orderRequest.getMoney()), "192.168.1.6");
            byte[] bytes = QrCodeUtil.generatePng(payUrl, 300, 300);
            OssClient storage = OssFactory.instance();
            UploadResult upload=storage.upload(bytes, storage.getPath("qrCode",".png"), "image/png");
            BeanUtil.copyProperties(payOrder,paymentOrdersVo);
            paymentOrdersVo.setUrl(upload.getUrl());
        }else {
            WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
            request.setTradeType("NATIVE");
            request.setBody(orderRequest.getName());
            request.setOutTradeNo(payOrder.getOrderNo());
            request.setTotalFee(BaseWxPayRequest.yuanToFen(orderRequest.getMoney()));
            request.setSpbillCreateIp("127.0.0.1");
            request.setNotifyUrl(getKey("notifyUrl"));
            request.setProductId(payOrder.getId().toString());
            try {
                WxPayNativeOrderResult order = wxService.createOrder(request);
                byte[] bytes = QrCodeUtil.generatePng(order.getCodeUrl(), 300, 300);
                OssClient storage = OssFactory.instance();
                UploadResult upload = storage.upload(bytes, storage.getPath("qrCode",".png"), "image/png");
                BeanUtil.copyProperties(payOrder,paymentOrdersVo);
                paymentOrdersVo.setUrl(upload.getUrl());
            } catch (WxPayException e) {
                throw new BaseException("获取微信支付二维码发生错误：{}"+e.getMessage());
            }
        }
        return R.ok(paymentOrdersVo);
    }

    /**
     * 回调通知地址
     *
     * @Date 2023/7/3
     * @param
     * @return void
     **/
    @GetMapping("/notifyUrl")
    public String notifyUrl(PayResponse payResponse) {
        // 校验签名
        String mdString = "money=" + payResponse.getMoney() + "&name=" + payResponse.getName() +
            "&out_trade_no=" + payResponse.getOut_trade_no() + "&pid=" + payConfig.getPid() +
            "&trade_no=" + payResponse.getTrade_no() + "&trade_status=" + payResponse.getTrade_status() +
            "&type=" + payResponse.getType() +  payConfig.getKey();
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
        sysUserVo.setUserBalance(sysUserVo.getUserBalance() + money);
        SysUserBo sysUserBo = new SysUserBo();
        BeanUtil.copyProperties(sysUserVo,sysUserBo);
        // 设置为付费用户
        sysUserBo.setUserGrade("1");
        userService.updateUser(sysUserBo);
        return "success";
    }

    /**
     * 跳转通知地址
     *
     * @Date 2023/7/3
     * @param
     * @return void
     **/
    @GetMapping("/return_url")
    public void returnUrl() {
        log.info("return_url===========");
    }


    @PostMapping("/notify/wxOrder")
    public String parseOrderNotifyResult(@RequestBody String xmlData) throws WxPayException {
        WxPayOrderNotifyResult notifyResult = this.wxService.parseOrderNotifyResult(xmlData);
        // TODO 根据自己业务场景需要构造返回对象
        PaymentOrdersBo paymentOrdersBo = new PaymentOrdersBo();
        paymentOrdersBo.setOrderNo(notifyResult.getOutTradeNo());
        List<PaymentOrdersVo> paymentOrdersList = paymentOrdersService.queryList(paymentOrdersBo);
        PaymentOrdersVo paymentOrdersVo = paymentOrdersList.get(0);
        paymentOrdersVo.setPaymentStatus("2");
        paymentOrdersVo.setPaymentMethod("wx");
        BeanUtil.copyProperties(paymentOrdersVo,paymentOrdersBo);
        paymentOrdersService.updateByBo(paymentOrdersBo);
        SysUserVo sysUserVo = userService.selectUserById(paymentOrdersVo.getUserId());
        sysUserVo.setUserBalance(sysUserVo.getUserBalance() + convertCentsToYuan(notifyResult.getTotalFee()));
        SysUserBo sysUserBo = new SysUserBo();
        BeanUtil.copyProperties(sysUserVo,sysUserBo);
        // 设置为付费用户
        sysUserBo.setUserGrade("1");
        userService.updateUser(sysUserBo);
        return WxPayNotifyResponse.success("success");
    }

    /**
     * 将分转换为元，并保留精度。
     *
     * @param cents 分的金额，类型为Integer
     * @return 转换后的元金额，类型为double
     */
    public static double convertCentsToYuan(Integer cents) {
        // 处理空输入
        if (cents == null) {
            throw new IllegalArgumentException("输入的分金额不能为空");
        }

        // 100分 = 1元
        BigDecimal centsBigDecimal = new BigDecimal(cents);
        BigDecimal yuan = centsBigDecimal.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        // 转换为double并返回
        return yuan.doubleValue();
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

    // 获取支付链接
//    static {
//        Stripe.apiKey = "sk_test_51PMMj2KcfX4oNioqXkoKpScTsgmR55xQki2tg8MEZJYc0gjhYV85t2FzDasE06eqZb0sqyYhOp3UXhcGGQLWI4A9008aq8SOnb";
//    }

    /**
     *   去支付
     * 1、创建产品
     * 2、设置价格
     * 3、创建支付信息 得到url
     * @return
     */
    @PostMapping("/stripePay")
    public String pay(@RequestBody OrderRequest orderRequest) throws StripeException {

        String enabled = configService.getConfigValue("stripe", "enabled");
        if(!Boolean.parseBoolean(enabled)){
            String prompt = configService.getConfigValue("stripe", "prompt");
            throw new BaseException(prompt);
        }

        // 获取支付链接
        Stripe.apiKey = configService.getConfigValue("stripe", "key");

        // 获取金额字符串并解析为 double
        double moneyDouble = Double.parseDouble(orderRequest.getMoney());

        // 将金额转换为以分为单位的整数
        int randMoney = (int) (moneyDouble * 100);

        Map<String, Object> params = new HashMap<>();
        params.put("name", orderRequest.getName());
        Product product = Product.create(params);

        Map<String, Object> recurring = new HashMap<>();
        recurring.put("interval", "month");
        Map<String, Object> params2 = new HashMap<>();
        params2.put("unit_amount", randMoney);
        params2.put("currency", "usd");
        params2.put("recurring", recurring);
        params2.put("product", product.getId());
        Price price = Price.create(params2);

        // 创建支付订单
        PaymentOrdersBo payOrder = paymentOrdersService.createPayOrder(orderRequest);

        //创建支付信息 得到url
        SessionCreateParams params3 = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(configService.getConfigValue("stripe", "success"))
            .setCancelUrl(configService.getConfigValue("stripe", "cancel"))
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPrice(price.getId())
                    .build()).putMetadata("orderId", payOrder.getOrderNo())
            .build();
        Session session = Session.create(params3);
        return session.getUrl();
    }

    /**
     *  支付回调
     *
     */
    @PostMapping("/stripe_events")
    public R<String> stripeEvent(HttpServletRequest request) {
        try {
            String endpointSecret = configService.getConfigValue("stripe", "secret");//webhook秘钥签名
            InputStream inputStream = request.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024*4];
            int n = 0;
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
            }
            byte[] bytes = output.toByteArray();
            String payload = new String(bytes, "UTF-8");
            String sigHeader = request.getHeader("Stripe-Signature");
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);//验签，并获取事件
            if("checkout.session.completed".equals(event.getType())){
                // 解析 JSON 字符串为 JSONObject
                JSONObject jsonObject = JSONUtil.parseObj(event);
                // 获取 metadata 对象
                JSONObject metadata = jsonObject.getJSONObject("data")
                    .getJSONObject("object")
                    .getJSONObject("metadata");

                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setPayType("stripe");
                orderRequest.setOrderNo(metadata.getStr("orderId"));
                paymentOrdersService.updatePayOrder(orderRequest);
            }
        } catch (Exception e) {
            System.out.println("stripe异步通知（webhook事件）"+e);
        }
        return R.ok();
    }

    public String getKey(String key) {
        return configService.getConfigValue("weixin", key);
    }

}

