package com.xmzs.common.chat.domain.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class RoleDataResponse {
    private String id;
    private String name;
    private String status;
    private String from;
    private String originId;
    private MetadataResponse metadata;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String userId;
}
