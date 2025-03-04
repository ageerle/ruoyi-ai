package org.ruoyi.system.domain.vo.cover;

import lombok.Data;

/**
 * 翻唱回调VO
 *
 * @author NSL
 * @since 2024-12-26
 */
@Data
public class CoverCallbackVo {
    /** 本次请求的订单号 */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 本次消费金额 */
    private String cost;

    /** 翻唱后的URL */
    private String coverUrl;
}
