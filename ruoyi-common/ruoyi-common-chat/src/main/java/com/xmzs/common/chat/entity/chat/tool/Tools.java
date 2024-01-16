package com.xmzs.common.chat.entity.chat.tool;


import lombok.*;

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
public class Tools implements Serializable {

    /**
     * 目前只支持：function
     *
     * @see Type
     */
    private String type;

    private ToolsFunction function;

    @Getter
    @AllArgsConstructor
    public enum Type {
        FUNCTION("function"),
        ;
        private final String name;
    }
}
