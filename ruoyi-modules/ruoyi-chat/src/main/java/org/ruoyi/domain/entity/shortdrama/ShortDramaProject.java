package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_project")
public class ShortDramaProject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long userId;

    private String projectName;

    private String description;

    private String status;

    private String artStyle;

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
