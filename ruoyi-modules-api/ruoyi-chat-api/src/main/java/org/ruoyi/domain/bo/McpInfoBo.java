package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.domain.McpInfo;

import java.io.Serializable;

/**
 * MCP业务对象 mcp_info
 *
 * @author ageerle
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@Data

@AutoMapper(target = McpInfo.class, reverseConvertGenerate = false)
public class McpInfoBo implements Serializable {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Integer mcpId;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 链接方式
     */
    private String transportType;

    /**
     * Command
     */
    private String command;

    /**
     * Args
     */
    private String arguments;
    private String description;
    /**
     * Env
     */
    private String env;

    /**
     * 是否启用
     */
    private Boolean status;


}
