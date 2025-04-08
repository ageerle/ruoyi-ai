package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

import java.io.Serial;

/**
 * 用户兑换记录对象 chat_voucher
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_voucher")
public class ChatVoucher extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 兑换金额
     */
    private BigDecimal amount;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 兑换状态
     */
    private String status;

    /**
     * 兑换前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 兑换后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 备注
     */
    private String remark;


}
