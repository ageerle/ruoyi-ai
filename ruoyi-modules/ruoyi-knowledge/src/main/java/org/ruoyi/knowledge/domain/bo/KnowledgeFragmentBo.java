package org.ruoyi.knowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.knowledge.domain.KnowledgeFragment;

/**
 * 知识片段业务对象 knowledge_fragment
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeFragment.class, reverseConvertGenerate = false)
public class KnowledgeFragmentBo extends BaseEntity {

    /**
     *
     */
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
     * 知识片段ID
     */
    @NotBlank(message = "知识片段ID不能为空")
    private String fid;

    /**
     * 片段索引下标
     */
    @NotNull(message = "片段索引下标不能为空")
    private Long idx;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;


}
