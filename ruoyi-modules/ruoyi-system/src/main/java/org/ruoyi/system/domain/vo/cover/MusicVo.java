package org.ruoyi.system.domain.vo.cover;

import lombok.Data;

/**
 * @author NSL
 * @since 2024-12-25
 */
@Data
public class MusicVo {
    /**
     * 歌曲名称
     */
    private String musicName;

    /**
     * 音乐地址
     */
    private String mp3Url;

    /**
     * 歌曲图片
     */
    private String pic;

    /**
     * 歌手或乐队名
     */
    private String singerName;

    /**
     * 歌手性别
     */
    private String singerGender;
}
