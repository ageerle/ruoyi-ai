package org.ruoyi.domain.request;

import lombok.Data;

/**
 *  翻译请求对象
 *
 * @author ageerle@163.com
 * date 2025/1/13
 */

@Data
public class TranslationRequest {
    /**
     * 提示词
     */
    private String prompt;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 目标语言
     */
    private String targetLanguage;

}
