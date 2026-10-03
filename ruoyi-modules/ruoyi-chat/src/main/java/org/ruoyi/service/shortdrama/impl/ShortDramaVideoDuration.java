package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Explicit video settings are separate from model-written planning estimates. */
public final class ShortDramaVideoDuration {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ShortDramaVideoDuration() { }

    public static Integer seconds(String continuity) {
        try {
            var root = JSON.readTree(continuity == null || continuity.isBlank() ? "{}" : continuity);
            if (root == null || !root.isObject()) throw new IllegalArgumentException("镜头承接信息须为JSON对象");
            var value = root.get("video_seconds");
            if (value == null || value.isNull()) return null;
            if (!value.isIntegralNumber() || !value.canConvertToInt()) throw new IllegalArgumentException("视频秒数须为正整数或-1；留空时不提交时长参数");
            int seconds = value.intValue();
            if (seconds != -1 && seconds <= 0) throw new IllegalArgumentException("视频秒数须为正整数或-1；留空时不提交时长参数");
            return seconds;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("镜头承接信息须为有效JSON", e);
        }
    }

    static Integer reviewSeconds(String continuity) {
        Integer seconds = seconds(continuity);
        return seconds != null && seconds > 0 ? seconds : null;
    }

    static String direction(String continuity) {
        Integer seconds = reviewSeconds(continuity);
        return seconds == null
            ? "[时长与内容] 未指定固定视频秒数，完整保留原剧情、对白和动作顺序；制作估算不作为删减依据，不为估算秒数压缩剧情或删掉末尾事件。\n"
            : "[用户指定视频时长] " + seconds + "秒；完整保留原剧情与对白，不擅自删减。\n";
    }
}
