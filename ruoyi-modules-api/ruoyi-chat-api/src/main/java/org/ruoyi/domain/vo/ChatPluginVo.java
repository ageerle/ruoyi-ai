package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatPlugin;

import java.io.Serial;
import java.io.Serializable;


/**
 * 插件管理视图对象 chat_plugin
 *
 * @author ageerle
 * @date 2025-03-30
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatPlugin.class)
public class ChatPluginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 插件名称
     */
    @ExcelProperty(value = "插件名称")
    private String name;

    /**
     * 插件编码
     */
    @ExcelProperty(value = "插件编码")
    private String code;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
