package org.ruoyi.chat.domain.bo;

import lombok.Data;

/**
 * 生成歌词
 *
 * @author ageerle@163.com
 * date 2024/6/27
 */
@Data
public class GenerateLyric {

    /**
     * 歌词提示词
     */
    private String prompt;

    /**
     * 回调地址
     */
    private String notify_hook;
}
