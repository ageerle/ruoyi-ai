package org.ruoyi.mcp.service.core;

/**
 * 内置工具定义
 * 用于描述系统内置的工具信息
 *
 * @param name        工具名称（唯一标识）
 * @param displayName 显示名称
 * @param description 工具描述
 * @author ruoyi team
 */
public record BuiltinToolDefinition(String name, String displayName, String description) {
}
