package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaStoryboard.class)
public class ShortDramaStoryboardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Date createTime;

    private Date updateTime;
}
