package com.xmzs.common.translation.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.xmzs.common.translation.core.handler.TranslationHandler;

import java.lang.annotation.*;

/**
 * 通用翻译注解
 *
 * @author Lion Li
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = TranslationHandler.class)
public @interface Translation {

    /**
     * 类型 (需与实现类上的 {@link TranslationType} 注解type对应)
     * <p>
     * 默认取当前字段的值 如果设置了 @{@link Translation#mapper()} 则取映射字段的值
     */
    String type();

    /**
     * 映射字段 (如果不为空则取此字段的值)
     */
    String mapper() default "";

    /**
     * 其他条件 例如: 字典type(sys_user_sex)
     */
    String other() default "";

}
