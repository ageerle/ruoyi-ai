package org.ruoyi.common.translation.annotation;

import org.ruoyi.common.translation.core.TranslationInterface;

import java.lang.annotation.*;

/**
 * 翻译类型注解 (标注到{@link TranslationInterface} 的实现类)
 *
 * @author Lion Li
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface TranslationType {

    /**
     * 类型
     */
    String type();

}
