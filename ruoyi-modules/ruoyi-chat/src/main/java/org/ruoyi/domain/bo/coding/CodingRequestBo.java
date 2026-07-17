package org.ruoyi.domain.bo.coding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 编程能力对话请求
 *
 * @author ageerle
 */
@Data
public class CodingRequestBo {

    /**
     * 用户指令
     */
    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    /**
     * 模型名称（走 IChatModelService.selectModelByName）
     */
    @NotBlank(message = "model 不能为空")
    private String model;

    /**
     * 工作目录，可选；为空时默认指向 ruoyi-copilot 前端项目
     */
    private String workspacePath;
}
