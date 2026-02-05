package org.ruoyi.workflow.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义条件注解，用于基于配置启用或禁用特定功能
 * <p>
 * 该注解只会在配置文件中 `warm-flow.enabled=true` 时，标注了此注解的类或方法才会被 Spring 容器加载
 * <p>
 * 示例配置：
 * <pre>
 * warm-flow:
 *   enabled: true  # 设置为 true 时，启用工作流功能
 * </pre>
 * <p>
 * 使用此注解时，可以动态控制工作流功能是否启用，而不需要修改代码逻辑
 *
 * @author Lion Li
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ConditionalOnProperty(value = "warm-flow.enabled", havingValue = "true")
public @interface ConditionalOnEnable {
}
