package org.ruoyi.common.core.validate.dicts;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.ruoyi.common.core.service.DictService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;

/**
 * 自定义字典值校验器
 *
 * @author AprilWind
 */
public class DictPatternValidator implements ConstraintValidator<DictPattern, String> {

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 分隔符
     */
    private String separator = ",";

    /**
     * 初始化校验器，提取注解上的字典类型
     *
     * @param annotation 注解实例
     */
    @Override
    public void initialize(DictPattern annotation) {
        this.dictType = annotation.dictType();
        if (StringUtils.isNotBlank(annotation.separator())) {
            this.separator = annotation.separator();
        }
    }

    /**
     * 校验字段值是否为指定字典类型中的合法值
     *
     * @param value   被校验的字段值
     * @param context 校验上下文（可用于构建错误信息）
     * @return true 表示校验通过（合法字典值），false 表示不通过
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(dictType) || StringUtils.isBlank(value)) {
            return false;
        }
        String dictLabel = SpringUtils.getBean(DictService.class).getDictLabel(dictType, value, separator);
        return StringUtils.isNotBlank(dictLabel);
    }

}
