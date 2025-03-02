package org.ruoyi.knowledge.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.knowledge.domain.KnowledgeFragment;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识片段视图对象 knowledge_fragment
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeFragment.class)
public class KnowledgeFragmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 知识库ID
     */
    @ExcelProperty(value = "知识库ID")
    private String kid;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private String docId;

    /**
     * 知识片段ID
     */
    @ExcelProperty(value = "知识片段ID")
    private String fid;

    /**
     * 片段索引下标
     */
    @ExcelProperty(value = "片段索引下标")
    private Long idx;

    /**
     * 文档内容
     */
    @ExcelProperty(value = "文档内容")
    private String content;

}
