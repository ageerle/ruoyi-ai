package org.ruoyi.mcp.service.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置工具注册表
 * 自动发现并注册所有实现 {@link BuiltinToolProvider} 接口的工具
 *
 * <p>工具注册流程：
 * <ol>
 *   <li>Spring 自动注入所有 {@link BuiltinToolProvider} 实现</li>
 *   <li>{@link #init()} 方法在 Bean 初始化后自动调用</li>
 *   <li>将所有工具注册到内部 Map</li>
 * </ol>
 *
 * <p>添加新工具只需：
 * <ol>
 *   <li>创建一个类实现 {@link BuiltinToolProvider} 接口</li>
 *   <li>添加 {@code @Component} 注解</li>
 *   <li>工具会自动被发现和注册</li>
 * </ol>
 *
 * @author ruoyi team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinToolRegistry {

    /**
     * 工具类型常量
     */
    public static final String TYPE_BUILTIN = "BUILTIN";

    /**
     * Spring 自动注入所有实现 BuiltinToolProvider 接口的 Bean
     */
    private final List<BuiltinToolProvider> toolProviders;

    /**
     * 内置工具定义映射表 (工具名称 -> 工具提供者)
     */
    private final Map<String, BuiltinToolProvider> registeredTools = new ConcurrentHashMap<>();

    /**
     * 初始化方法，在 Bean 创建后自动调用
     * 将所有 BuiltinToolProvider 注册到内部 Map
     */
    @PostConstruct
    public void init() {
        log.info("开始注册内置工具，发现 {} 个工具提供者", toolProviders.size());

        for (BuiltinToolProvider provider : toolProviders) {
            String toolName = provider.getToolName();

            if (registeredTools.containsKey(toolName)) {
                log.warn("工具名称重复: {}，将覆盖原有注册", toolName);
            }

            registeredTools.put(toolName, provider);
            log.info("注册内置工具: {} ({})", toolName, provider.getDisplayName());
        }

        log.info("内置工具注册完成，共 {} 个工具", registeredTools.size());
    }

    /**
     * 获取工具提供者
     *
     * @param toolName 工具名称
     * @return 工具提供者，如果不存在则返回 null
     */
    public BuiltinToolProvider getToolProvider(String toolName) {
        return registeredTools.get(toolName);
    }

    /**
     * 检查工具是否已注册
     *
     * @param toolName 工具名称
     * @return 是否已注册
     */
    public boolean hasTool(String toolName) {
        return registeredTools.containsKey(toolName);
    }

    /**
     * 获取所有内置工具定义
     *
     * @return 内置工具定义集合
     */
    public Collection<BuiltinToolDefinition> getAllBuiltinTools() {
        return registeredTools.values().stream()
            .map(provider -> new BuiltinToolDefinition(
                provider.getToolName(),
                provider.getDisplayName(),
                provider.getDescription()
            ))
            .toList();
    }

    /**
     * 获取所有内置工具对象
     * 这些对象包含 @Tool 注解的方法，可直接用于 AgenticServices
     *
     * @return 内置工具对象列表
     */
    public List<Object> getAllBuiltinToolObjects() {
        return List.copyOf(registeredTools.values());
    }

    /**
     * 根据工具名称获取工具对象
     *
     * @param toolName 工具名称
     * @return 工具对象，如果不存在则返回 null
     */
    public Object getBuiltinToolObject(String toolName) {
        return registeredTools.get(toolName);
    }
}
