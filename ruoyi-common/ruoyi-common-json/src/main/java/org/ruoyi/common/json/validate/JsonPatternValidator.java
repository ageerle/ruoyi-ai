package org.ruoyi.common.json.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;

/**
 * JSON 格式校验器
 *
 * @author AprilWind
 */
public class JsonPatternValidator implements ConstraintValidator<JsonPattern, String> {

    /**
     * 注解中指定的 JSON 类型枚举
     */
    private JsonType jsonType;

    /**
     * 初始化校验器，从注解中提取 JSON 类型
     *
     * @param annotation 注解实例
     */
    @Override
    public void initialize(JsonPattern annotation) {
        this.jsonType = annotation.type();
    }

    /**
     * 校验字符串是否为合法 JSON
     *
     * @param value   待校验字符串
     * @param context 校验上下文，可用于自定义错误信息
     * @return true = 合法 JSON 或为空，false = 非法 JSON
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            // 交给 @NotBlank 或 @NotNull 控制是否允许为空
            return true;
        }
        // 根据 JSON 类型进行不同的校验
        return switch (jsonType) {
            case ANY -> JsonUtils.isJson(value);
            case OBJECT -> JsonUtils.isJsonObject(value);
            case ARRAY -> JsonUtils.isJsonArray(value);
        };
    }

}
