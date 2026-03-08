package org.ruoyi.domain.bo.mcp;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.mcp.McpMarket;

/**
 * MCP 市场业务对象
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = McpMarket.class, reverseConvertGenerate = false)
public class McpMarketBo extends BaseEntity {

    /**
     * 市场ID
     */
    private Long id;

    /**
     * 市场名称
     */
    @NotBlank(message = "市场名称不能为空")
    @Size(min = 0, max = 200, message = "市场名称不能超过{max}个字符")
    private String name;

    /**
     * 市场 URL
     */
    @NotBlank(message = "市场URL不能为空")
    @Size(min = 0, max = 500, message = "市场URL不能超过{max}个字符")
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
