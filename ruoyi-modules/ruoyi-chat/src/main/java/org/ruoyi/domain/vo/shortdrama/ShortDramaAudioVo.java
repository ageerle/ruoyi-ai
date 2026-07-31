package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaAudio;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaAudio.class)
public class ShortDramaAudioVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String name;

    private String audioType;

    private String text;

    private String voice;

    private Long audioOssId;

    private String audioUrl;

    private Long linkedStoryboardId;

    private Integer durationSeconds;

    private Date createTime;

    private Date updateTime;
}
