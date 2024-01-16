package com.xmzs.common.core.constant;

/**
 * 租户常量信息
 *
 * @author Lion Li
 */
public interface TenantConstants {

    /**
     * 租户正常状态
     */
    String NORMAL = "0";

    /**
     * 租户封禁状态
     */
    String DISABLE = "1";

    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员角色 roleKey
     */
    String SUPER_ADMIN_ROLE_KEY = "superadmin";

    /**
     * 租户管理员角色 roleKey
     */
    String TENANT_ADMIN_ROLE_KEY = "admin";

    /**
     * 租户管理员角色名称
     */
    String TENANT_ADMIN_ROLE_NAME = "管理员";

    /**
     * 默认租户ID
     */
    String DEFAULT_TENANT_ID = "000000";

}
