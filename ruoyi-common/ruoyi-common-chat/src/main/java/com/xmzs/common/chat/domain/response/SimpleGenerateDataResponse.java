package com.xmzs.common.chat.domain.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class SimpleGenerateDataResponse {

    /**
     * 本次生成的ID
     */
    private String id;

    /**
     * 本次生成结果的音频文件地址
     */
    private String audio;

    /**
     * 本次生成所消耗的点数
     */
    private Integer credit_used;
}
