package org.ruoyi.chat.domain.bo;


import lombok.Data;

import java.io.Serializable;

/**
 * @author WangLe
 */
@Data
public class GenerateSuno implements Serializable {

    /**
     * 歌词 (自定义模式专用)
     */
    private String prompt;

    /**
     * mv模型，chirp-v3-0、chirp-v3-5。不写默认 chirp-v3-0
     */
    private String mv;

    /**
     * 标题(自定义模式专用)
     */
    private String title;

    /**
     * 风格标签(自定义模式专用)
     */
    private String tags;

    /**
     * 是否生成纯音乐，true 为生成纯音乐
     */
    private boolean make_instrumental;

    /**
     * 任务id，用于对之前的任务再操作
     */
    private String task_id;

    /**
     * float，歌曲延长时间，单位秒
     */
    private int continue_at;

    /**
     * 歌曲id，需要续写哪首歌
     */
    private String continue_clip_id;

    /**
     * 灵感模式提示词(灵感模式专用)
     */
    private String gpt_description_prompt;

    /**
     * 回调地址
     */
    private String notify_hook;

}
