package org.ruoyi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识片段对象 knowledge_fragment
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@TableName("knowledge_fragment")
public class KnowledgeFragment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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


}
