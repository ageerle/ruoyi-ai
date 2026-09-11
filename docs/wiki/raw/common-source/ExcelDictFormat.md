---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-common/ruoyi-common-excel/src/main/java/org/ruoyi/common/excel/annotation/ExcelDictFormat.java
collected: 2026-09-04
published: 2026-09-04
topic: common-source
---

# ExcelDictFormat.java

```java
package org.ruoyi.common.excel.annotation;

import org.ruoyi.common.core.utils.StringUtils;

import java.lang.annotation.*;

/**
 * 字典格式化
 *
 * @author Lion Li
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelDictFormat {

    /**
     * 如果是字典类型，请设置字典的type值 (如: sys_user_sex)
     */
    String dictType() default "";

    /**
     * 读取内容转表达式 (如: 0=男,1=女,2=未知)
     */
    String readConverterExp() default "";

    /**
     * 分隔符，读取字符串组内容
     */
    String separator() default StringUtils.SEPARATOR;

}

```
