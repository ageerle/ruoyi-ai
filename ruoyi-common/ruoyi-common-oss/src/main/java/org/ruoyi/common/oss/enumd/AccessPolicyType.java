package org.ruoyi.common.oss.enumd;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 桶访问策略配置
 *
 * @author 陈賝
 */
@Getter
@AllArgsConstructor
public enum AccessPolicyType {

    /**
     * private
     */
    PRIVATE("0", CannedAccessControlList.Private, PolicyType.WRITE),

    /**
     * public
     */
    PUBLIC("1", CannedAccessControlList.PublicRead, PolicyType.READ),

    /**
     * custom
     */
    CUSTOM("2",CannedAccessControlList.PublicRead, PolicyType.READ);

    /**
     * 桶 权限类型
     */
    private final String type;

    /**
     * 文件对象 权限类型
     */
    private final CannedAccessControlList acl;

    /**
     * 桶策略类型
     */
    private final PolicyType policyType;

    public static AccessPolicyType getByType(String type) {
        for (AccessPolicyType value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new RuntimeException("'type' not found By " + type);
    }

}
