package org.ruoyi.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.sensitive.annotation.Sensitive;
import org.ruoyi.common.sensitive.core.SensitiveStrategy;
import org.ruoyi.domain.ChatModel;

import java.io.Serial;
import java.io.Serializable;



/**
 * 聊天模型视图对象 chat_model
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatModel.class)
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
     * 模型描述
     */
    @ExcelProperty(value = "模型描述")
    private String modelDescribe;

    /**
     * 模型价格
     */
    @ExcelProperty(value = "模型价格")
    private Double modelPrice;

    /**
     * 计费类型
     */
    @ExcelProperty(value = "计费类型")
    private String modelType;

    /**
     * 是否显示
     */
    @ExcelProperty(value = "是否显示")
    private String modelShow;

    /**
     * 模型维度
     */
    private Integer dimension;

    /**
     * 系统提示词
     */
    @ExcelProperty(value = "系统提示词")
    private String systemPrompt;

    /**
     * 请求地址
     */
    @ExcelProperty(value = "请求地址")
    private String apiHost;

    /**
     * 密钥
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ExcelProperty(value = "密钥")
    private String apiKey;

    /**
     * 优先级(值越大优先级越高)
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

    /**
     * 模型供应商
     */
    @ExcelProperty(value = "模型供应商")
    private String ProviderName;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}