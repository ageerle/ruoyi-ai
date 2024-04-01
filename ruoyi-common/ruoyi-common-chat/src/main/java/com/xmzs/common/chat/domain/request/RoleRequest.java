package com.xmzs.common.chat.domain.request;

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
     * 角色描述
     */
    private String description;

    /**
     * 音频地址
     */
    private String prompt;

    /**
     * 头像
     */
    private String avatar;


    private String preProcess;

}
