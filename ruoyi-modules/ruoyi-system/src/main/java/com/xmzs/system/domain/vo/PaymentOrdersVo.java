package com.xmzs.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.xmzs.system.domain.PaymentOrders;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;



/**
 * 支付订单视图对象 payment_orders
 *
 * @author Lion Li
 * @date 2023-12-29
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PaymentOrders.class)
public class PaymentOrdersVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 订单编号
     */
    @ExcelProperty(value = "订单编号")
    private String orderNo;

    /**
     * 订单名称
     */
    @ExcelProperty(value = "订单名称")
    private String orderName;

    /**
     * 金额
     */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /**
     * 支付状态
     */
    @ExcelProperty(value = "支付状态")
    private String paymentStatus;

    /**
     * 支付方式
     */
    @ExcelProperty(value = "支付方式")
    private String paymentMethod;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 二维码地址
     */
    private String url;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
