package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.ChatConfig;

import java.io.Serial;
import java.io.Serializable;



/**
 * 对话配置信息
视图对象 chat_config
 *
 * @author Lion Li
 * @date 2024-04-13
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatConfig.class)
public class ChatConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 配置类型
     */
    @ExcelProperty(value = "配置类型")
    private String category;

    /**
     * 配置名称
     */
    @ExcelProperty(value = "配置名称")
    private String configName;

    /**
     * 配置值
     */
    @ExcelProperty(value = "配置值")
    private String configValue;

    /**
     * 说明
     */
    @ExcelProperty(value = "说明")
    private String configDict;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 更新IP
     */
    @ExcelProperty(value = "更新IP")
    private String updateIp;


}
