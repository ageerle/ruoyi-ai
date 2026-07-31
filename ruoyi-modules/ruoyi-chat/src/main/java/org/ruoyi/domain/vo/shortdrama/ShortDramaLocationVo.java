package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaLocation;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaLocation.class)
public class ShortDramaLocationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Date createTime;

    private Date updateTime;
}
