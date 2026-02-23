package org.ruoyi.mcp.service.core;

/**
 * 内置工具提供者接口
 * 所有系统内置工具都应实现此接口，以便自动注册到 BuiltinToolRegistry
 *
 * @author ruoyi team
 *
 * <p>使用方式：
 * <pre>
 * {@code
 * @Component
 * public class MyTool implements BuiltinToolProvider {
 *     @Override
 *     public String getToolName() {
 *         return "my_tool";
 *     }
 *
 *     @Override
 *     public String getDisplayName() {
 *         return "我的工具";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "工具描述...";
 *     }
 * }
 * }
 * </pre>
 */
public interface BuiltinToolProvider {

    /**
     * 获取工具名称（唯一标识，用于数据库存储）
     * 建议使用 snake_case 格式，如：list_directory, edit_file
     *
     * @return 工具名称
     */
    String getToolName();

    /**
     * 获取工具显示名称（用于 UI 展示）
     *
     * @return 显示名称
     */
    String getDisplayName();

    /**
     * 获取工具描述（用于 AI 理解工具用途）
     *
     * @return 工具描述
     */
    String getDescription();
}
