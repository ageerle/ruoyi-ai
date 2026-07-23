package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = ShortDramaCharacter.class)
public class ShortDramaCharacterVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private List<ShortDramaCharacterAppearanceVo> appearances;

    private Date createTime;

    private Date updateTime;
}
