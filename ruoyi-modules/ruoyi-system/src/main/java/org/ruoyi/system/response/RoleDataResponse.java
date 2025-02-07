package org.ruoyi.system.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class RoleDataResponse {
    /**
     * 语音角色 ID
     */
    private String id;

    /**
     * 音角色名称
     */
    private String name;

    /**
     * 语音角色状态，可以为
     * pending（瞬时克隆已完成）
     * lora-pending（专业克隆训练中）
     * lora-success（专业克隆已完成）
     * lora-failed（专业克隆失败）
     */
    private String status;
    private MetadataResponse metadata;
    private String from;
    private String originId;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String userId;
}
