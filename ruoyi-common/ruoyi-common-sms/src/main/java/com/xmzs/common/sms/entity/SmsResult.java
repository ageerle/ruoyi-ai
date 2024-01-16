package com.xmzs.common.sms.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 上传返回体
 *
 * @author Lion Li
 */
@Data
@Builder
public class SmsResult {

    /**
     * 是否成功
     */
    private boolean isSuccess;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 实际响应体
     * <p>
     * 可自行转换为 SDK 对应的 SendSmsResponse
     */
    private String response;
}
