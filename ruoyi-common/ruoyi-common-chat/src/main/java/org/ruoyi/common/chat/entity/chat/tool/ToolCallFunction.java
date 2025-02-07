package org.ruoyi.common.chat.entity.chat.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ToolCall 的 Function参数
 * The function that the model called.
 *
 * @author https:www.unfbx.com
 * @since 1.1.2
 * 2023-11-09
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolCallFunction implements Serializable {
    /**
     * 方法名
     */
    private String name;
    /**
     * 方法参数
     */
    private String arguments;
}
