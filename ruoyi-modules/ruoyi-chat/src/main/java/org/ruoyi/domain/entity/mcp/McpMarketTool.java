package org.ruoyi.domain.entity.mcp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * MCP 市场工具关联实体
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_market_tool")
public class McpMarketTool extends BaseEntity {

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 市场 ID
     */
    private Long marketId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String toolDescription;

    /**
     * 工具版本
     */
    private String toolVersion;

    /**
     * 工具元数据（JSON格式）
     */
    private String toolMetadata;

    /**
     * 是否已加载到本地
     */
    private Boolean isLoaded;

    /**
     * 关联的本地工具 ID
     */
    private Long localToolId;

}
