package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaProject.class, reverseConvertGenerate = false)
public class ShortDramaProjectBo extends BaseEntity {

    private Long id;

    private Long userId;

    private String projectName;

    private String description;

    private String status;

    private String artStyle;

    private String composeAspectRatio;
}
