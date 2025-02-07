package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 配音角色对象 voice_role
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_audio_role")
public class VoiceRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色id
     */
    private String voiceId;

    /**
     * 音频地址
     */
    private String fileUrl;


    /**
     * 备注
     */
    private String remark;


}
