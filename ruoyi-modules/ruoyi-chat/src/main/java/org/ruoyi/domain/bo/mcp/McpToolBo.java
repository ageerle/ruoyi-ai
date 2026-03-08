package org.ruoyi.domain.bo.mcp;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.mcp.McpTool;

import java.io.Serial;

/**
 * MCP 工具业务对象
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = McpTool.class, reverseConvertGenerate = false)
public class McpToolBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    private Long id;

    /**
     * 工具名称
     */
    @NotBlank(message = "工具名称不能为空")
    @Size(min = 0, max = 200, message = "工具名称不能超过{max}个字符")
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具类型：LOCAL-本地, REMOTE-远程, BUILTIN-内置
     */
    @NotBlank(message = "工具类型不能为空")
    private String type;

    /**
     * 状态：ENABLED-启用, DISABLED-禁用
     */
    private String status;

    /**
     * 配置信息（JSON格式）
     */
    private String configJson;

}
