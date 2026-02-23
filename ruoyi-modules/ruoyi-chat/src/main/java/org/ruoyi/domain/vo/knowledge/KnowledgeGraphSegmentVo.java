package org.ruoyi.domain.vo.knowledge;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.knowledge.KnowledgeGraphSegment;

import java.io.Serial;
import java.io.Serializable;


/**
 * 知识图谱片段视图对象 knowledge_graph_segment
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeGraphSegment.class)
public class KnowledgeGraphSegmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 片段UUID
     */
    @ExcelProperty(value = "片段UUID")
    private String uuid;

    /**
     * 知识库UUID
     */
    @ExcelProperty(value = "知识库UUID")
    private String kbUuid;

    /**
     * 知识库条目UUID
     */
    @ExcelProperty(value = "知识库条目UUID")
    private String kbItemUuid;

    /**
     * 文档UUID
     */
    @ExcelProperty(value = "文档UUID")
    private String docUuid;

    /**
     * 片段文本内容
     */
    @ExcelProperty(value = "片段文本内容")
    private String segmentText;

    /**
     * 片段索引（第几个片段）
     */
    @ExcelProperty(value = "片段索引", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "第=几个片段")
    private Long chunkIndex;

    /**
     * 总片段数
     */
    @ExcelProperty(value = "总片段数")
    private Long totalChunks;

    /**
     * 抽取状态：0-待处理 1-处理中 2-已完成 3-失败
     */
    @ExcelProperty(value = "抽取状态：0-待处理 1-处理中 2-已完成 3-失败")
    private Long extractionStatus;

    /**
     * 抽取的实体数量
     */
    @ExcelProperty(value = "抽取的实体数量")
    private Long entityCount;

    /**
     * 抽取的关系数量
     */
    @ExcelProperty(value = "抽取的关系数量")
    private Long relationCount;

    /**
     * 消耗的token数
     */
    @ExcelProperty(value = "消耗的token数")
    private Long tokenUsed;

    /**
     * 错误信息
     */
    @ExcelProperty(value = "错误信息")
    private String errorMessage;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
