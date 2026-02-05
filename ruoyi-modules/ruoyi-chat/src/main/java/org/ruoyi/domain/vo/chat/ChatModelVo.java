package org.ruoyi.domain.vo.chat;

import org.ruoyi.domain.entity.chat.ChatModel;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

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
     * 模型价格
     */
    @ExcelProperty(value = "模型价格")
    private Long modelPrice;

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
     * 是否免费
     */
    @ExcelProperty(value = "是否免费")
    private String modelFree;

    /**
     * 模型优先级(值越大优先级越高)
     */
    @ExcelProperty(value = "模型优先级(值越大优先级越高)")
    private Long priority;

    /**
     * 请求地址
     */
    @ExcelProperty(value = "请求地址")
    private String apiHost;

    /**
     * 密钥
     */
    @ExcelProperty(value = "密钥")
    private String apiKey;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 模型维度
     */
    private Integer dimension;


}
