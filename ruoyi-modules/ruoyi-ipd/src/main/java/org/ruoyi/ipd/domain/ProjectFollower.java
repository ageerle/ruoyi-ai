package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 项目协作圈成员——TS 闭链域（原型 project_followers）。
 * 圈角色 FOLLOWER|COMMENTER|CONTRIBUTOR；管理人边界见 ProjectCircleService#canManage。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_followers", autoResultMap = true)
public class ProjectFollower extends BaseEntity implements SoftDeletable {

    public static final String ROLE_FOLLOWER = "FOLLOWER";
    public static final String ROLE_COMMENTER = "COMMENTER";
    public static final String ROLE_CONTRIBUTOR = "CONTRIBUTOR";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    /** 人员ID（persons.id） */
    @TableField("user_id")
    private Long userId;

    /** FOLLOWER|COMMENTER|CONTRIBUTOR */
    @TableField("circle_role")
    private String circleRole;

    /** 添加人 */
    @TableField("added_by")
    private Long addedBy;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
