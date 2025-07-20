package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识库角色与知识库关联对象 knowledge_role_relation
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_role_relation")
public class KnowledgeRoleRelation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * 知识库角色id
     */
    private Long knowledgeRoleId;

    /**
     * 知识库id
     */
    private Long knowledgeId;


}
