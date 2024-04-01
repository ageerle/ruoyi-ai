package com.xmzs.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xmzs.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 支付订单对象 payment_orders
 *
 * @author Lion Li
 * @date 2023-12-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_orders")
public class PaymentOrders extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单名称
     */
    private String orderName;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 备注
     */
    private String remark;

}
