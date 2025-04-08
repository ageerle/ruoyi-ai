package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.ChatVoucher;


import java.math.BigDecimal;

/**
 * 用户兑换记录业务对象 chat_voucher
 *
 * @author Lion Li
 * @date 2024-05-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatVoucher.class, reverseConvertGenerate = false)
public class ChatVoucherBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
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
    @NotNull(message = "兑换金额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal amount;

    /**
     * 兑换状态
     */
    private String status;

    /**
     * 兑换前余额
     */
    private Double balanceBefore;

    /**
     * 兑换后余额
     */
    private Double balanceAfter;

    /**
     * 备注
     */
    private String remark;


}
