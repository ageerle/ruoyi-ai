package com.xmzs.system.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    /**
     * 商品金额
     */
    @NotNull(message = "商品金额")
    private String money;

    /**
     * 商品名称
     */
    @NotNull(message = "商品名称")
    private String name;

    /**
     * 订单编号
     */
    @NotNull(message = "订单编号")
    private String orderNo;
}
