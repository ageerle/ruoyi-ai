package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识片段对象 knowledge_fragment
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_fragment")
public class KnowledgeFragment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 知识库ID
     */
    private String kid;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 知识片段ID
     */
    private String fid;

    /**
     * 片段索引下标
     */
    private Integer idx;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 备注
     */
    private String remark;


}
