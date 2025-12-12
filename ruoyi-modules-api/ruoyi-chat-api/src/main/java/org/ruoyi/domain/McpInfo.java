package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

/**
 * MCP对象 mcp_info
 *
 * @author ageerle
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_info")
public class McpInfo extends BaseEntity {


    /**
     * id
     */
    @TableId(value = "mcp_id", type = IdType.AUTO)
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
