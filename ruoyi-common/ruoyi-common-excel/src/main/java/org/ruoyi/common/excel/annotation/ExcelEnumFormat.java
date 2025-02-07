package org.ruoyi.common.excel.annotation;

import java.lang.annotation.*;

/**
 * 枚举格式化
 *
 * @author Liang
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelEnumFormat {

    /**
     * 字典枚举类型
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 字典枚举类中对应的code属性名称，默认为code
     */
    String codeField() default "code";

    /**
     * 字典枚举类中对应的text属性名称，默认为text
     */
    String textField() default "text";

}
