package org.ruoyi.common.chat.entity.chat.tool;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.common.chat.entity.chat.Parameters;

import java.io.Serializable;

/**
 * @author <a href="https://www.unfbx.com">unfbx</a>
 * @since 1.1.2
 * 2023-11-09
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolsFunction implements Serializable {

    /**
     * 要调用的函数的名称。必须是 a-z、A-Z、0-9，或包含下划线和破折号，最大长度为 64
     */
    private String name;
    /**
     * 对函数功能的描述，模型使用它来选择何时以及如何调用该函数。
     */
    private String description;
    /**
     * 函数接受的参数，描述为 JSON Schema 对象
     * 扩展参数可以继承Parameters自己实现，json格式的数据
     */
    private Parameters parameters;
}
