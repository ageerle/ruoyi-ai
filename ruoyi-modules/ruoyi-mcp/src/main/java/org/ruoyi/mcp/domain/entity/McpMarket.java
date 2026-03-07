package org.ruoyi.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * MCP 市场信息实体
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_market_info")
public class McpMarket extends TenantEntity {

    /**
     * 市场ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 市场名称
     */
    private String name;

    /**
     * 市场 URL
     */
    private String url;

    /**
     * 市场描述
     */
    private String description;

    /**
     * 认证配置（JSON格式）
     */
    private String authConfig;

    /**
     * 状态：ENABLED-启用, DISABLED-禁用
     */
    private String status;

}
