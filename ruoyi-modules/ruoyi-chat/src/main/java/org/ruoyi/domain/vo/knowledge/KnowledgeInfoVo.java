package org.ruoyi.domain.vo.knowledge;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.knowledge.KnowledgeInfo;

import java.io.Serial;
import java.io.Serializable;



/**
 * 知识库视图对象 knowledge_info
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeInfo.class)
public class KnowledgeInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 知识库名称
     */
    @ExcelProperty(value = "知识库名称")
    private String name;

    /**
     * 是否公开知识库（0 否 1是）
     */
    @ExcelProperty(value = "是否公开知识库", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=,否=,1=是")
    private Long share;

    /**
     * 知识库描述
     */
    @ExcelProperty(value = "知识库描述")
    private String description;

    /**
     * 知识分隔符
     */
    @ExcelProperty(value = "知识分隔符")
    private String separator;

    /**
     * 重叠字符数
     */
    @ExcelProperty(value = "重叠字符数")
    private Long overlapChar;

    /**
     * 知识库中检索的条数
     */
    @ExcelProperty(value = "知识库中检索的条数")
    private Integer retrieveLimit;

    /**
     * 相似度阈值
     */
    @ExcelProperty(value = "相似度阈值")
    private Double similarityThreshold;

    /**
     * 文本块大小
     */
    @ExcelProperty(value = "文本块大小")
    private Long textBlockSize;

    /**
     * 向量库
     */
    @ExcelProperty(value = "向量库")
    private String vectorModel;

    /**
     * 向量模型
     */
    @ExcelProperty(value = "向量模型")
    private String embeddingModel;

    /**
     * 是否启用重排序（0 否 1是）
     */
    @ExcelProperty(value = "是否启用重排序")
    private Integer enableRerank;

    /**
     * 重排序模型名称
     */
    @ExcelProperty(value = "重排序模型")
    private String rerankModel;

    /**
     * 重排序后返回的文档数量
     */
    @ExcelProperty(value = "重排序返回数量")
    private Integer rerankTopN;

    /**
     * 重排序相关性分数阈值
     */
    @ExcelProperty(value = "重排序分数阈值")
    private Double rerankScoreThreshold;

    /**
     * 是否启用混合检索（0 否 1是）
     */
    @ExcelProperty(value = "是否启用混合检索")
    private Integer enableHybrid;

    /**
     * 混合检索权重 (0.0-1.0)
     */
    @ExcelProperty(value = "混合检索权重")
    private Double hybridAlpha;

    /**
     * 文档数量
     */
    @ExcelProperty(value = "文档数量")
    private Integer documentCount;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
