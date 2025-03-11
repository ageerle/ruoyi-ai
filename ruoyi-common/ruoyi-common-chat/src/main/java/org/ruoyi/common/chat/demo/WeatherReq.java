package org.ruoyi.common.chat.demo;


import lombok.Data;
import org.ruoyi.common.chat.openai.plugin.PluginParam;

@Data
public class WeatherReq extends PluginParam {
    /**
     * 城市
     */
    private String location;
}
