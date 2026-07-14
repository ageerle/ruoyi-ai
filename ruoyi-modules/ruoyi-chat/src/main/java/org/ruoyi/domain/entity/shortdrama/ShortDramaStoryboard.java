package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_storyboard")
public class ShortDramaStoryboard extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private Long scriptId;

    private Integer sceneNo;

    private String sceneTitle;

    private String sceneText;

    private String sceneType;

    private String shotType;

    private String cameraMove;

    private String charactersJson;

    private String locationName;

    private String photographyRules;

    private String actingNotes;

    private String continuityJson;

    private String sourceText;

    private String imagePrompt;

    private Integer durationSeconds;

    private String videoPrompt;

    private String videoUrl;

    private String videoId;

    private String videoStatus;
}
