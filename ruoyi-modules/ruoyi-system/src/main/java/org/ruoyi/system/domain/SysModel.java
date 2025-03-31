package org.ruoyi.system.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 系统模型对象 sys_model
 *
 * @author Lion Li
 * @date 2024-04-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_model")
public class SysModel extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 模型分类
     */
    @ExcelProperty(value = "模型分类")
    private String category;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型描述
     */
    private String modelDescribe;

    /**
     * 模型价格
     */
    private double modelPrice;

    /**
     * 计费类型
     */
    private String modelType;

    /**
     * 是否显示
     */
    private String modelShow;


    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 请求地址
     */
    private String apiHost;

    /**
     * 密钥
     */
    private String apiKey;

    /**
     * 备注
     */
    private String remark;

}
