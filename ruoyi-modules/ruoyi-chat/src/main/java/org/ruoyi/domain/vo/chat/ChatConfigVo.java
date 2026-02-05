package org.ruoyi.domain.vo.chat;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.chat.ChatConfig;

import java.io.Serial;
import java.io.Serializable;



/**
 * 配置信息视图对象 chat_config
 *
 * @author ageerle
 * @date 2025-12-14
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
