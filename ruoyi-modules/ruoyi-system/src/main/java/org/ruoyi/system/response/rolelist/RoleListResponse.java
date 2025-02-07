package org.ruoyi.system.response.rolelist;

import lombok.Data;

import java.util.List;

/**
 * @author WangLe
 */
@Data
public class RoleListResponse {
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
    private List<ContentResponse> data;
}
