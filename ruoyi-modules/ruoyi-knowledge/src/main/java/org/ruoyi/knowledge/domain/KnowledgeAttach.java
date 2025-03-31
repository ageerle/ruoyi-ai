package org.ruoyi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识库附件对象 knowledge_attach
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data

@TableName("knowledge_attach")
public class KnowledgeAttach  extends BaseEntity {


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
     * 文档名称
     */
    private String docName;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 文档内容
     */
    private String content;

}
