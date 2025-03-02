package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 翻唱用户参考音频对象
 *
 * @author NSL
 * @since  2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_cover_prompt_audio")
public class CoverPromptAudio extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;
    /**
     * 翻唱主表id
     */
    private Long coverId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 参考音频
     */
    private String promptAudioUrl;
}
