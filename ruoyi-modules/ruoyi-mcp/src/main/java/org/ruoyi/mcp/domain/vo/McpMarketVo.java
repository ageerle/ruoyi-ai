package org.ruoyi.mcp.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.mcp.domain.entity.McpMarket;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * MCP 市场视图对象
 *
 * @author ruoyi team
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = McpMarket.class)
public class McpMarketVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 市场ID
     */
    @ExcelProperty(value = "市场ID")
    private Long id;

    /**
     * 市场名称
     */
    @ExcelProperty(value = "市场名称")
    private String name;

    /**
     * 市场 URL
     */
    @ExcelProperty(value = "市场URL")
    private String url;

    /**
     * 市场描述
     */
    @ExcelProperty(value = "市场描述")
    private String description;

    /**
     * 认证配置
     */
    @ExcelProperty(value = "认证配置")
    private String authConfig;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

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
