package com.xmzs.common.chat.domain.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class RoleResponse {
    private String status;
    private String message;
    private RoleDataResponse data;
}
