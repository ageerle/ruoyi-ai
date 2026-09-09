package org.ruoyi.common.chat.domain.vo.chat;


import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.common.chat.security.ChatModelCredentialPolicy;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型管理视图对象 chat_model
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatModel.class, convertGenerate = false)
public class ChatModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 模型分类
     */
    @ExcelProperty(value = "模型分类")
    private String category;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;

    /**
     * 模型供应商
     */
    @ExcelProperty(value = "模型供应商")
    private String providerCode;

    /**
     * 模型描述
     */
    @ExcelProperty(value = "模型描述")
    private String modelDescribe;


    /**
     * 是否显示
     */
    @ExcelProperty(value = "是否显示")
    private String modelShow;

    /**
     * 向量维度
     */
    @ExcelProperty(value = "向量维度")
    private Integer modelDimension;

    /**
     * 请求地址
     */
    @ExcelProperty(value = "请求地址")
    private String apiHost;

    /**
     * 密钥仅用于服务端模型调用和写入配置。任何读取接口、日志字符串或 Excel 导出
     * 都不得把它带出服务端边界。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKey;

    /** Returns the stored configuration reference without touching the process environment. */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Resolves the reference only after binding it to both the consuming provider and this row's
     * effective provider, model and endpoint. Non-allowlisted, misrouted, or legacy rows fail
     * closed before the environment is read.
     */
    public String resolveApiKeyForConfiguredEndpoint(String consumingProviderCode) {
        return ChatModelCredentialPolicy.resolveApiKeyForUse(
            consumingProviderCode, providerCode, modelName, apiHost, apiKey);
    }

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}

