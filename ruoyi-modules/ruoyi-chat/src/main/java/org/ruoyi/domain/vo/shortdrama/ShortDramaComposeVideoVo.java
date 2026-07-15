package org.ruoyi.domain.vo.shortdrama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortDramaComposeVideoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;

    private String status;

    private Integer progress;

    private String transitionType;

    private BigDecimal transitionDurationSeconds;

    private String aspectRatio;

    /** Actual duration measured from the final MP4 with ffprobe. */
    private BigDecimal outputDurationSeconds;

    private Long videoOssId;

    /** Freshly resolved URL; private-bucket URLs may be short lived. */
    private String videoUrl;

    private String errorMessage;

    private Date composedAt;
}
