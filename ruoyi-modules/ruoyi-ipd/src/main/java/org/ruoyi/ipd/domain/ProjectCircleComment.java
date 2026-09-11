package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 项目协作圈评论——原型 project_circle_comments（支持 parentId 楼中楼）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_circle_comments", autoResultMap = true)
public class ProjectCircleComment extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("post_id")
    private Long postId;

    /** 作者（persons.id） */
    @TableField("author_id")
    private Long authorId;

    /** 父评论（可空，楼中楼） */
    @TableField("parent_id")
    private Long parentId;

    /** 评论内容 2-2000 */
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
