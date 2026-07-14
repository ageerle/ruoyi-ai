package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_location")
public class ShortDramaLocation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String name;

    private String summary;

    private Boolean hasCrowd;

    private String crowdDescription;

    private String availableSlots;

    private String descriptions;

    private String referenceImageUrl;

    /** 生成图片URL列表（JSON数组） */
    private String imageUrls;

    /** 每张图片对应的提示词（JSON数组） */
    private String imageDescriptions;

    /** 当前选中的图片索引 */
    private Integer selectedImageIndex;

    /** 上一轮图片URL列表（撤销用，JSON数组） */
    private String previousImageUrls;

    /** 上一轮提示词列表（撤销用，JSON数组） */
    private String previousDescriptions;
}
