package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

import java.io.Serial;

/**
 * 用户兑换记录对象 chat_voucher
 *
 * @author Lion Li
 * @date 2024-05-03
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
     * 用户id
     */
    private Long userId;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 兑换金额
     */
    private BigDecimal amount;

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
