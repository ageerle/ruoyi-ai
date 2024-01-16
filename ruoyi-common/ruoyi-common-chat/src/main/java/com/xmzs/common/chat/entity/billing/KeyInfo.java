package com.xmzs.common.chat.entity.billing;

import lombok.*;

import java.time.LocalDate;

/**
 * openKey信息
 *
 * @author admin
 * @date 2023/6/15
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KeyInfo {
    /**
     * 订阅类型
     */
    private String planTitle;
    /**
     * key值
     */
    private String keyValue;
    /**
     * 剩余额度
     */
    private Double remaining;

    /**
     * 账户总余额
     */
    private Double totalAmount;

    /**
     * 已使用的额度
     */
    private Double totalUsage;

    /**
     * 截至日期
     */
    private LocalDate limitDate;

    /**
     * 是否绑卡
     */
    private Boolean isHasPaymentMethod;

    /**
     * 最高可用模型
     */
    private String model;
}
