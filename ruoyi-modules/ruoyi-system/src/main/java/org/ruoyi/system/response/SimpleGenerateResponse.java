package org.ruoyi.system.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class SimpleGenerateResponse {

    /**
     * 状态码，失败时则为500
     */
    private String status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 生成详情
     */
    private SimpleGenerateDataResponse data;
}
