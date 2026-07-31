package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_audio")
public class ShortDramaAudio extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    /** 语音资产名称 */
    private String name;

    /** 语音类型：narration(旁白)/dialogue(对白) */
    private String audioType;

    /** 语音文案（生成语音用的文本） */
    private String text;

    /** 音色（如 alloy/onyx） */
    private String voice;

    /** 音频文件OSS ID */
    private Long audioOssId;

    /** 音频文件URL */
    private String audioUrl;

    /** 对白关联的分镜ID（NULL=全局旁白） */
    private Long linkedStoryboardId;

    /** 音频时长（秒） */
    private Integer durationSeconds;
}
