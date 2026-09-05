package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 销售回款台账（P3-4.1 AC-INC-16b/16c/16d/31/31b/32）
 * 月度回款/退款冲减/6自然月窗口/凭证可追
 * 回款口径 salesSource=RECEIPT（不是出库/开票）
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "receipt_ledger", autoResultMap = true)
public class ReceiptLedger extends BaseEntity implements SoftDeletable {

    /** 主键（雪花算法） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 奖金池ID（关联 bonus_pools.id） */
    private Long bonusPoolId;

    /** 回款月份 YYYY-MM */
    private String receiptMonth;

    /** 回款金额（正数） */
    private BigDecimal receiptAmount;

    /** 退款冲减金额（正数，AC-INC-31b 窗口内当期冲减） */
    private BigDecimal refundAmount;

    /** 净回款 = receiptAmount - refundAmount（生成列，只读） */
    @TableField(exist = false)
    private BigDecimal netAmount;

    /** 凭证附件URL（银行回单/对账单，AC-INC-16c） */
    private String voucherUrl;

    /** 凭证SHA256 */
    private String voucherHash;

    /** 来源 RECEIPT|SHIPMENT（AC-INC-16b 口径验证：必须是 RECEIPT） */
    private String source;

    /** 6自然月窗口起算日（上市日期，AC-INC-32） */
    private Date windowStart;

    /** 6自然月窗口截止日 */
    private Date windowEnd;

    /** 是否在窗口内（生成列，只读；AC-INC-16d 窗外不计） */
    @TableField(exist = false)
    private Boolean inWindow;

    /** 软删除标志（0正常 1已删） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
