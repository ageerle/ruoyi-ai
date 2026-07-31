package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaLocation;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaLocation.class, reverseConvertGenerate = false)
public class ShortDramaLocationBo extends BaseEntity {

    private Long id;

    private Long projectId;

    private String name;

    private String summary;

    private Boolean hasCrowd;

    private String crowdDescription;

    private String availableSlots;

    private String descriptions;

    private String referenceImageUrl;

    private String imageUrls;

    private String imageDescriptions;

    private Integer selectedImageIndex;

    private String previousImageUrls;

    private String previousDescriptions;
}
