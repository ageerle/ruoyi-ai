package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.domain.McpInfo;

import java.io.Serializable;


/**
 * MCP视图对象 mcp_info
 *
 * @author jiyi
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = McpInfo.class)
public class McpInfoVo implements Serializable {
    private Integer mcpId;

    /**
     * 服务器名称
     */
    @ExcelProperty(value = "服务器名称")
    private String serverName;

    /**
     * 链接方式
     */
    @ExcelProperty(value = "链接方式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "mcp_transport_type")
    private String transportType;

    /**
     * Command
     */
    @ExcelProperty(value = "Command")
    private String command;

    /**
     * Args
     */
    @ExcelProperty(value = "Args")
    private String arguments;
    @ExcelProperty(value = "Description")
    private String description;
    /**
     * Env
     */
    @ExcelProperty(value = "Env")
    private String env;

    /**
     * 是否启用
     */
    @ExcelProperty(value = "是否启用")
    private Boolean status;


}
