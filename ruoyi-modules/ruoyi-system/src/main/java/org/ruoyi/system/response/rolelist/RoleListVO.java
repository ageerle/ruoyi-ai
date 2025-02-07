package org.ruoyi.system.response.rolelist;

import lombok.Data;

/**
 * 描述：
 *
 * @author ageerle@163.com
 * date 2024/4/27
 */
@Data
public class RoleListVO {


    private String name;

    private String description;

    private String voicesId;

    private String avatar;

    private String previewAudio;

    public RoleListVO(String name, String description, String voicesId, String previewAudio,String avatar) {
        this.name = name;
        this.description = description;
        this.voicesId = voicesId;
        this.previewAudio = previewAudio;
        this.avatar = avatar;
    }
}
