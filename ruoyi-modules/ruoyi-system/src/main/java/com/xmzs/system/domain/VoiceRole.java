package com.xmzs.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xmzs.common.mybatis.core.domain.BaseEntity;
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
@TableName("voice_role")
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
     * 音频预处理（实验性）
     */
    private String preProcess;

    /**
     * 备注
     */
    private String remark;


}
