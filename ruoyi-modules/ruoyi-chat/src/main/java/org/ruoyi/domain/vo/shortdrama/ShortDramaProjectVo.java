package org.ruoyi.domain.vo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = ShortDramaProject.class)
public class ShortDramaProjectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String projectName;

    private String description;

    private String status;

    private String artStyle;

    private Long composedVideoOssId;

    private String composeStatus;

    private Integer composeProgress;

    private String composeTransitionType;

    private BigDecimal composeTransitionDurationSeconds;

    private String composeAspectRatio;

    private BigDecimal composedVideoDurationSeconds;

    private String composeErrorMessage;

    private Date composedAt;

    private Date createTime;

    private Date updateTime;
}
