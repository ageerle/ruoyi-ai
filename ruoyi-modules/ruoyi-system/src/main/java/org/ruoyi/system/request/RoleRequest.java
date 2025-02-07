package org.ruoyi.system.request;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class RoleRequest {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色默认风格音频样本，base64 编码的音频数据
     */
    private String prompt;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 专业克隆样本Zip文件的分片上传ID，请先通过分片上传接口完成文件上传
     */
    private String lora;

}
