package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 知识库业务对象 knowledge_info
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeInfo.class, reverseConvertGenerate = false)
public class KnowledgeInfoBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 知识库ID
     */
    @NotBlank(message = "知识库ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String kid;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long uid;

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String kname;

    /**
     * 是否公开知识库（0 否 1是）
     */
    @NotNull(message = "是否公开知识库（0 否 1是）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer share;

    /**
     * 描述
     */
    @NotBlank(message = "描述不能为空", groups = { AddGroup.class, EditGroup.class })
    private String description;

    /**
     * 知识分隔符
     */
    @NotBlank(message = "知识分隔符不能为空", groups = { AddGroup.class, EditGroup.class })
    private String knowledgeSeparator;

    /**
     * 提问分隔符
     */
    @NotBlank(message = "提问分隔符不能为空", groups = { AddGroup.class, EditGroup.class })
    private String questionSeparator;

    /**
     * 重叠字符数
     */
    @NotNull(message = "重叠字符数不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long overlapChar;

    /**
     * 知识库中检索的条数
     */
    @NotNull(message = "知识库中检索的条数不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long retrieveLimit;

    /**
     * 文本块大小
     */
    @NotNull(message = "文本块大小不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long textBlockSize;

    /**
     * 向量库
     */
    @NotBlank(message = "向量库不能为空", groups = { AddGroup.class, EditGroup.class })
    private String vector;

    /**
     * 向量模型
     */
    @NotBlank(message = "向量模型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String vectorModel;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
