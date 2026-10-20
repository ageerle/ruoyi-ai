package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "short_drama_project", autoResultMap = true)
public class ShortDramaProject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long userId;

    private String projectName;

    private String description;

    /** Original user input, independent of generated summaries and script revisions. */
    private String originalIdea;

    private String status;

    private String artStyle;
    private String aestheticSkillName;
    private String directorSkillName;
    /** Ordered supplementary skills for storyboard planning and review. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private java.util.List<String> storyboardSkillNames;

    private Long composedVideoOssId;

    private String composeStatus;

    private String composeJobId;

    private Integer composeProgress;

    private String composeTransitionType;

    private BigDecimal composeTransitionDurationSeconds;

    private String composeAspectRatio;

    private BigDecimal composedVideoDurationSeconds;

    private String composeErrorMessage;

    private Date composedAt;
}
