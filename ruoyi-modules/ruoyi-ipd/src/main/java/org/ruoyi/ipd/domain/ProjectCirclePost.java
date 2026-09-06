package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 项目协作圈动态——原型 project_circle_posts。
 * objectType/objectId 可关联需求/变更/闸门等业务对象；content 2-3000 字。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_circle_posts", autoResultMap = true)
public class ProjectCirclePost extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    /** 作者（persons.id） */
    @TableField("author_id")
    private Long authorId;

    /** 关联对象类型（可空） */
    @TableField("object_type")
    private String objectType;

    /** 关联对象ID（可空） */
    @TableField("object_id")
    private Long objectId;

    /** 动态内容 2-3000 */
    @TableField("content")
    private String content;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
