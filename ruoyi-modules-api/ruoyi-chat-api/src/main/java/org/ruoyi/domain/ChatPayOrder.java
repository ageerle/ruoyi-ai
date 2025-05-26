package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 支付订单对象 chat_pay_order
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_pay_order")
public class ChatPayOrder extends BaseEntity {

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
