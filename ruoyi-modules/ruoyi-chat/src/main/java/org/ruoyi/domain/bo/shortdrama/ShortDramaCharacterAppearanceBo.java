package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaCharacterAppearance.class, reverseConvertGenerate = false)
public class ShortDramaCharacterAppearanceBo extends BaseEntity {

    private Long id;

    private Long characterId;

    private Integer appearanceIndex;

    private String changeReason;

    private String description;

    private String referenceImageUrl;

    private String imageUrls;

    private String imageDescriptions;

    private Integer selectedImageIndex;

    private String previousImageUrls;

    private String previousDescriptions;
}
