package org.ruoyi.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识库附件对象 knowledge_attach
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_attach")
public class KnowledgeAttach extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 文档ID-用于关联文本块信息
     */
    private String docId;

    /**
     * 附件名称
     */
    private String name;

    /**
     * 附件类型
     */
    private String type;

    /**
     * 对象存储ID
     */
    private Long ossId;

    /**
     * 备注
     */
    private String remark;


}
