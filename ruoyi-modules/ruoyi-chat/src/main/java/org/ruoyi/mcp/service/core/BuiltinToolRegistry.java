package org.ruoyi.mcp.service.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

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
     * 注意：这些是 Spring 代理，不能直接用于 LangChain4j
     * 我们需要提取 Class 信息以便创建新实例
     */
    private final List<BuiltinToolProvider> toolProviders;

    /**
     * 内置工具类映射表 (工具名称 -> 工具类)
     * 存储 Class 对象而不是实例，以便创建不带 Spring 代理的新实例
     */
    private final Map<String, Class<?>> registeredToolClasses = new ConcurrentHashMap<>();

    /**
     * 内置工具显示名称映射表 (工具名称 -> 显示名称)
     */
    private final Map<String, String> displayNames = new ConcurrentHashMap<>();

    /**
     * 初始化方法，在 Bean 创建后自动调用
     * 提取工具类信息而不是存储 Spring 代理实例
     */
    @PostConstruct
    public void init() {
        log.info("开始注册内置工具，发现 {} 个工具提供者", toolProviders.size());

        // 1. 注册通过 Spring 自动发现工具
        for (BuiltinToolProvider provider : toolProviders) {
            String toolName = provider.getToolName();

            if (registeredToolClasses.containsKey(toolName)) {
                log.warn("工具名称重复: {}，将覆盖原有注册", toolName);
            }

            // 使用 ClassUtils.getUserClass 获取原始类，避免 Spring CGLIB 代理类
            Class<?> targetClass = ClassUtils.getUserClass(provider);
            registeredToolClasses.put(toolName, targetClass);
            displayNames.put(toolName, provider.getDisplayName());
            log.info("注册内置工具: {} ({}) - 原始类: {}", toolName, provider.getDisplayName(), targetClass.getName());
        }

        log.info("内置工具注册完成，共 {} 个工具", registeredToolClasses.size());
    }

    /**
     * 获取工具提供者（返回 Spring 代理，仅用于元数据查询）
     *
     * @param toolName 工具名称
     * @return 工具提供者，如果不存在则返回 null
     */
    public BuiltinToolProvider getToolProvider(String toolName) {
        // 这个方法返回 Spring 代理，仅用于获取元数据
        for (BuiltinToolProvider provider : toolProviders) {
            if (provider.getToolName().equals(toolName)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 检查工具是否已注册
     *
     * @param toolName 工具名称
     * @return 是否已注册
     */
    public boolean hasTool(String toolName) {
        return registeredToolClasses.containsKey(toolName);
    }

    /**
     * 获取所有内置工具定义
     *
     * @return 内置工具定义集合
     */
    public Collection<BuiltinToolDefinition> getAllBuiltinTools() {
        return displayNames.entrySet().stream()
            .map(entry -> new BuiltinToolDefinition(
                entry.getKey(),
                entry.getValue(),
                "" // Description can be added later if needed
            ))
            .toList();
    }

    /**
     * 获取所有内置工具对象
     * 这些对象包含 @Tool 注解的方法，可直接用于 AgenticServices
     * 注意：每次调用都创建新实例，以避免 Spring CGLIB 代理问题
     *
     * @return 内置工具对象列表
     */
    public List<Object> getAllBuiltinToolObjects() {
        List<Object> toolInstances = new java.util.ArrayList<>();

        for (java.util.Map.Entry<String, Class<?>> entry : registeredToolClasses.entrySet()) {
            try {
                // 使用无参构造函数创建新实例，保留 @Tool 注解
                Object instance = entry.getValue().getDeclaredConstructor().newInstance();
                toolInstances.add(instance);
                log.debug("创建工具实例: {}", entry.getKey());
            } catch (Exception e) {
                log.error("创建工具实例失败: {} - {}", entry.getKey(), e.getMessage());
            }
        }

        return toolInstances;
    }

    /**
     * 根据工具名称获取工具对象
     * 注意：每次调用都创建新实例，以避免 Spring CGLIB 代理问题
     *
     * @param toolName 工具名称
     * @return 工具对象，如果不存在则返回 null
     */
    public Object getBuiltinToolObject(String toolName) {
        Class<?> toolClass = registeredToolClasses.get(toolName);
        if (toolClass == null) {
            return null;
        }

        try {
            // 使用无参构造函数创建新实例，保留 @Tool 注解
            return toolClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("创建工具实例失败: {} - {}", toolName, e.getMessage());
            return null;
        }
    }
}
