package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import java.util.Date;

/**
 * IPD 人员（v3 TS-06 全字段）
 * 角色固定不可跨（B7）；等级仅 HR API 权威源（B6）；组长随 API 同步（A3）
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@TableName(value = "persons", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Person extends BaseEntity implements SoftDeletable {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("name")
    private String name;
    @TableField("employee_no")
    private String employeeNo;
    /** MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN */
    @TableField("person_type")
    private String personType;
    @TableField("group_id")
    private Long groupId;
    /** L1..L5 */
    @TableField("level")
    private String level;
    @TableField("level_source")
    private String levelSource;
    @TableField("level_updated_at")
    private Date levelUpdatedAt;
    /** ACTIVE|FROZEN_PENDING_HANDOVER|DISABLED|RESIGNED */
    @TableField("account_status")
    private String accountStatus;
    /** ACTIVE|RESIGNED */
    @TableField("employment_status")
    private String employmentStatus;
    @TableField("wecom_user_id")
    private String wecomUserId;
    @TableField("wecom_bound_at")
    private Date wecomBoundAt;
    @TableField("username")
    private String username;
    @TableField("password_hash")
    private String passwordHash;
    @TableField("must_change_pwd")
    private String mustChangePwd;
    @TableField("last_login_at")
    private Date lastLoginAt;
    @TableField("tenant_id")
    private String tenantId;
    @TableLogic @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok 生成的链式 setter（Person 无 @Accessors(chain=true) 但为对称保留显式覆盖）。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
    @TableField("remark")
    private String remark;
}