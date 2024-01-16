package com.xmzs.common.chat.entity.chat;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
/**
 * 描述：方法参数类，扩展参数可以继承Parameters自己实现
 * 参考：
 * <pre>
 * {
 *     "type": "object",
 *     "properties": {
 *         "location": {
 *             "type": "string",
 *             "description": "The city and state, e.g. San Francisco, CA"
 *         },
 *         "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]}
 *     },
 *     "required": ["location"]
 * }
 * </pre>
 * @author https:www.unfbx.com
 * @since  2023-06-14
 */
@Data
@Builder
public class Parameters implements Serializable {
    /**
     * 参数类型
     */
    private String type;
    /**
     * 参数属性、描述
     */
    private Object properties;
    /**
     * 方法必输字段
     */
    private List<String> required;
}
