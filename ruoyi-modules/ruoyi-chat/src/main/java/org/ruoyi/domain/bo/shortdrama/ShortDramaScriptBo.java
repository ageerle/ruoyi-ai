package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaScript.class, reverseConvertGenerate = false)
public class ShortDramaScriptBo extends BaseEntity {

    private Long id;

    private Long projectId;

    private String scriptName;

    private String scriptText;

    private String outlineText;

    private String tone;

    private String sourceType;
}
