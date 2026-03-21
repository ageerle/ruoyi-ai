package org.ruoyi.common.json.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * JSON 格式校验注解
 *
 * @author AprilWind
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonPatternValidator.class)
public @interface JsonPattern {

    /**
     * 限制 JSON 类型，默认为 {@link JsonType#ANY}，即对象或数组都允许
     */
    JsonType type() default JsonType.ANY;

    /**
     * 校验失败时的提示消息
     */
    String message() default "不是有效的 JSON 格式";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
