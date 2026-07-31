package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_character")
public class ShortDramaCharacter extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
