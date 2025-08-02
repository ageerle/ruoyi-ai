package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;
import java.util.List;

/**
 * 知识库角色对象 knowledge_role
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_role")
public class KnowledgeRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库角色id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 知识库角色组id
     */
    private Long groupId;

    /**
     * 知识库角色name
     */
    private String name;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    // @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;


    /**
     * 知识库id列表
     */
    @TableField(exist = false)
    private List<Long> knowledgeIds;

}
