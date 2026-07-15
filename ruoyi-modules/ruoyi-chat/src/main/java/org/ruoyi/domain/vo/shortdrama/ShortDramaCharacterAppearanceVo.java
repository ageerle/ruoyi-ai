package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaCharacterAppearance.class)
public class ShortDramaCharacterAppearanceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Date createTime;

    private Date updateTime;
}
