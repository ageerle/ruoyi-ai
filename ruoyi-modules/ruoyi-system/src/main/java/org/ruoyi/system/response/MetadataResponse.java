package org.ruoyi.system.response;

import lombok.Data;

import java.util.List;

/**
 * @author WangLe
 */
@Data
public class MetadataResponse {

    /**
     * 语音角色头像 URL
     */
    private String avatar;

    /**
     * 语音角色描述
     */
    private String description;

    /**
     * 语音角色风格列表
     */
    private List<PromptResponse> prompts;
}
