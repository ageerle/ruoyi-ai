package org.ruoyi.common.chat.plugin;


import lombok.Data;
import org.ruoyi.common.chat.openai.plugin.PluginParam;

@Data
public class SqlReq extends PluginParam {
    /**
     * 用户名称
     */
    private String username;
}
