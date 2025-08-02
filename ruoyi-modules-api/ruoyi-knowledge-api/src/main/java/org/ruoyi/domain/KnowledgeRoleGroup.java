package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识库角色组对象 knowledge_role_group
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_role_group")
public class KnowledgeRoleGroup extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库角色组id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 知识库角色组name
     */
    private String name;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;


}
