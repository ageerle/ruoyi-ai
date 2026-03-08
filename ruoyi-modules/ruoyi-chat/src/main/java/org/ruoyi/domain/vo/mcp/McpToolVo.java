package org.ruoyi.domain.vo.mcp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.mcp.McpTool;

import java.io.Serializable;
import java.util.Date;

/**
 * MCP 工具视图对象
 *
 * @author ruoyi team
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = McpTool.class)
public class McpToolVo implements Serializable {

    /**
     * 工具ID
     */
    @ExcelProperty(value = "工具ID")
    private Long id;

    /**
     * 工具名称
     */
    @ExcelProperty(value = "工具名称")
    private String name;

    /**
     * 工具描述
     */
    @ExcelProperty(value = "工具描述")
    private String description;

    /**
     * 工具类型
     */
    @ExcelProperty(value = "工具类型")
    private String type;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 配置信息
     */
    @ExcelProperty(value = "配置信息")
    private String configJson;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
