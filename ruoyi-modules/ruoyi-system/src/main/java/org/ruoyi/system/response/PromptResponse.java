package org.ruoyi.system.response;

import lombok.Data;

/**
 * 描述：语音角色风格列表
 *
 * @author ageerle@163.com
 * date 2024/4/27
 */
@Data
public class PromptResponse {

    /**
     * 角色风格 ID
     */
    private String id;

    /**
     * 角色风格名称
     */
    private String name;

    /**
     * 角色风格样本音频 URL
     */
    private String promptOriginAudioStorageUrl;

}
