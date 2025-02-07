package org.ruoyi.system.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class RoleResponse {
    /**
     * 状态码
     */
    private String status;
    /**
     * 状态信息
     */
    private String message;
    /**
     * 创建的语音角色详情
     */
    private RoleDataResponse data;
}
