package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.ChatVoucher;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 用户兑换记录业务对象 chat_voucher
 *
 * @author ageerle
 * @date 2025-04-08
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
     * 兑换码
     */
    @NotBlank(message = "兑换码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String code;

    /**
     * 兑换金额
     */
    @NotNull(message = "兑换金额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal amount;

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 兑换状态
     */
    @NotBlank(message = "兑换状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 兑换前余额
     */
    @NotNull(message = "兑换前余额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal balanceBefore;

    /**
     * 兑换后余额
     */
    @NotNull(message = "兑换后余额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal balanceAfter;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
