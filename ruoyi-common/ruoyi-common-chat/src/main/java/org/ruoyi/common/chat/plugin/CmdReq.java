package org.ruoyi.common.chat.plugin;


import lombok.Data;
import org.ruoyi.common.chat.openai.plugin.PluginParam;

@Data
public class CmdReq extends PluginParam {
    /**
     * 指令
     */
    private String cmd;
}
