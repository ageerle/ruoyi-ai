package com.xmzs.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

/**
 * 支付结果响应
 *
 * @author: wangle
 * @date: 2023/7/3
 */
@Data
public class PayResponse {

    /**
     * 商户ID
     */
    private String pid;

    /**
     * 易支付订单号
     */

    @JsonProperty("trade_no")
    private String trade_no;

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    private String out_trade_no;

    /**
     * 支付方式
     */
    private String type;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品金额
     */
    private String money;

    /**
     * 支付状态
     */
    @JsonProperty("trade_status")
    private String trade_status;

    /**
     * 业务扩展参数
     */
    private String param;

    /**
     * 签名字符串
     */
    private String sign;

    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    private String signType;

}
