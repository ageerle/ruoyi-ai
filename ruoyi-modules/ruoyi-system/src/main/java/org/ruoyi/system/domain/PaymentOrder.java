package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

import java.io.Serial;

/**
 * 支付订单对象 payment_orders
 *
 * @author Lion Li
 * @date 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_pay_order")
public class PaymentOrder extends BaseEntity {

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
