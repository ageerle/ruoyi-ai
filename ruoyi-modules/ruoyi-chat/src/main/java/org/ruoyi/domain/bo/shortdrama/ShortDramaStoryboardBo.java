package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaStoryboard.class, reverseConvertGenerate = false)
public class ShortDramaStoryboardBo extends BaseEntity {

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
}
