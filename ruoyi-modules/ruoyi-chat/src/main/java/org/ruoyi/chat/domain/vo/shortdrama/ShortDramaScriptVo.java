package org.ruoyi.chat.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.chat.domain.ShortDramaScript;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaScript.class)
public class ShortDramaScriptVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String scriptName;

    private String scriptText;

    private String outlineText;

    private String tone;

    private String sourceType;

    private Date createTime;

    private Date updateTime;

    private String creationMode;

    private String sourceMaterials;

    private String worldbuilding;

    private String revisionNotes;
}
