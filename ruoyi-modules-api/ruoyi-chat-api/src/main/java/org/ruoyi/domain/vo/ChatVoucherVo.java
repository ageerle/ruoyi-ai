package org.ruoyi.domain.vo;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatVoucher;

import java.io.Serial;
import java.io.Serializable;



/**
 * 用户兑换记录视图对象 chat_voucher
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatVoucher.class)
public class ChatVoucherVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 兑换码
     */
    @ExcelProperty(value = "兑换码")
    private String code;

    /**
     * 兑换金额
     */
    @ExcelProperty(value = "兑换金额")
    private BigDecimal amount;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 兑换状态
     */
    @ExcelProperty(value = "兑换状态")
    private String status;

    /**
     * 兑换前余额
     */
    @ExcelProperty(value = "兑换前余额")
    private BigDecimal balanceBefore;

    /**
     * 兑换后余额
     */
    @ExcelProperty(value = "兑换后余额")
    private BigDecimal balanceAfter;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
