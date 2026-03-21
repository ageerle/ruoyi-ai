package org.ruoyi.common.chat.domain.dto.request;

import lombok.Data;

/**
 * 人机交互输入信息
 */
@Data
public class ReSumeRunner {
    /**
     * 运行节点UUID
     */
    private String runtimeUuid;

    /**
     * 人机交互用户输入信息
     */
    private String feedbackContent;
}
