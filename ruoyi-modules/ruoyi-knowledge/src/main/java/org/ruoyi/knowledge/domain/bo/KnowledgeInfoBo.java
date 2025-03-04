package org.ruoyi.knowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.knowledge.domain.KnowledgeInfo;

/**
 * 知识库业务对象 knowledge_info
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeInfo.class, reverseConvertGenerate = false)
public class KnowledgeInfoBo extends BaseEntity {

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
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long uid;

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    private String kname;

    /**
     * 描述
     */
    @NotBlank(message = "描述不能为空")
    private String description;


}
