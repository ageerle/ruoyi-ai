package org.ruoyi.common.trace.annotation;

import org.ruoyi.common.trace.constant.TraceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trace 方法节点标记。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceNode {

    /**
     * 节点名称。
     */
    String name() default "";

    /**
     * 节点类型。
     */
    String type() default TraceConstants.NODE_METHOD;

    /**
     * 安全输入摘要。
     */
    String input() default "";
}
