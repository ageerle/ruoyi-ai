package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 回款台账月度录入请求（P3-4.1 AC-INC-16c）。
 * source 由服务端强制 RECEIPT（AC-INC-16b），不从请求接收；
 * windowStart 不从请求接收——服务端按项目上市日期起算 6 自然月窗口（AC-INC-32）。
 */
public record ReceiptLedgerCreateReq(
    @NotNull(message = "projectId 不能为空")
    Long projectId,
    @NotBlank(message = "回款月份不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "回款月份必须为 YYYY-MM 格式")
    String receiptMonth,
    @NotNull(message = "回款金额不能为空")
    @DecimalMin(value = "0.01", message = "回款金额必须为正数")
    BigDecimal receiptAmount,
    @PositiveOrZero(message = "退款金额不能为负数")
    BigDecimal refundAmount,
    @Size(max = 500, message = "凭证地址长度不能超过 500")
    String voucherUrl,
    @Size(max = 64, message = "凭证哈希长度不能超过 64")
    String voucherHash
) {
}
