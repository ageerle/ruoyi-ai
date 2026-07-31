package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaCharacter.class, reverseConvertGenerate = false)
public class ShortDramaCharacterBo extends BaseEntity {

    private Long id;

    private Long projectId;

    private String name;

    private String aliases;

    private String introduction;

    private String roleLevel;

    private String gender;

    private String ageRange;

    private String personalityTags;

    private Integer costumeTier;

    private String visualDescription;

    private String referenceImageUrl;
}
