package org.ruoyi.domain.entity.shortdrama;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_drama_character_appearance")
public class ShortDramaCharacterAppearance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long characterId;

    private Integer appearanceIndex;

    private String changeReason;

    private String description;

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

    /** 音色名（如 zh_male_taocheng_uranus_bigtts），用于该形象的对白配音 */
    private String voice;
}
