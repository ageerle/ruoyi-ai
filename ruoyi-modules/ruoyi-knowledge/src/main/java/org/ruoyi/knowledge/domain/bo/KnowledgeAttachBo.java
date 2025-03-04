package org.ruoyi.knowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.knowledge.domain.KnowledgeAttach;

/**
 * 知识库附件业务对象 knowledge_attach
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeAttach.class, reverseConvertGenerate = false)
public class KnowledgeAttachBo extends BaseEntity {

    @NotNull(message = "不能为空")
    private Long id;

    /**
     * 知识库ID
     */
    @NotBlank(message = "知识库ID不能为空")
    private String kid;

    /**
     * 文档ID
     */
    @NotBlank(message = "文档ID不能为空")
    private String docId;

    /**
     * 文档名称
     */
    @NotBlank(message = "文档名称不能为空")
    private String docName;

    /**
     * 文档类型
     */
    @NotBlank(message = "文档类型不能为空")
    private String docType;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;


}
