package org.ruoyi.domain.entity.mcp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;


/**
 * MCP 工具信息实体
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_tool_info")
public class McpTool extends TenantEntity {

    /**
     * 工具ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具类型：LOCAL-本地, REMOTE-远程, BUILTIN-内置
     */
    private String type;

    /**
     * 状态：ENABLED-启用, DISABLED-禁用
     */
    private String status;

    /**
     * 配置信息（JSON格式）
     * LOCAL: {"command": "npx", "args": ["-y", "@example/mcp-server"], "env": {...}}
     * REMOTE: {"baseUrl": "http://localhost:8080/mcp"}
     * BUILTIN: null
     */
    private String configJson;

}
