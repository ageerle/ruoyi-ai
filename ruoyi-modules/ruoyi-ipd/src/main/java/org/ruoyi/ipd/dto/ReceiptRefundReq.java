package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 退款冲减请求（P3-4.1 AC-INC-31/31b）。
 * 窗口内退款当期冲减；窗口外退款服务端拒绝（不做回溯扣减）。
 */
public record ReceiptRefundReq(
    @NotBlank(message = "冲减月份不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "冲减月份必须为 YYYY-MM 格式")
    String month,
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须为正数")
    BigDecimal refundAmount
) {
}
