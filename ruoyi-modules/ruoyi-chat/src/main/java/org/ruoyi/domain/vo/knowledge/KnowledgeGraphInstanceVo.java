package org.ruoyi.domain.vo.knowledge;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.knowledge.KnowledgeGraphInstance;

import java.io.Serial;
import java.io.Serializable;


/**
 * 知识图谱实例视图对象 knowledge_graph_instance
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeGraphInstance.class)
public class KnowledgeGraphInstanceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 图谱UUID
     */
    @ExcelProperty(value = "图谱UUID")
    private String graphUuid;

    /**
     * 关联knowledge_info.kid
     */
    @ExcelProperty(value = "关联knowledge_info.kid")
    private String knowledgeId;

    /**
     * 图谱名称
     */
    @ExcelProperty(value = "图谱名称")
    private String graphName;

    /**
     * 构建状态：10构建中、20已完成、30失败
     */
    @ExcelProperty(value = "构建状态：10构建中、20已完成、30失败")
    private Long graphStatus;

    /**
     * 节点数量
     */
    @ExcelProperty(value = "节点数量")
    private Long nodeCount;

    /**
     * 关系数量
     */
    @ExcelProperty(value = "关系数量")
    private Long relationshipCount;

    /**
     * 图谱配置(JSON格式)
     */
    @ExcelProperty(value = "图谱配置(JSON格式)")
    private String config;

    /**
     * LLM模型名称
     */
    @ExcelProperty(value = "LLM模型名称")
    private String modelName;

    /**
     * 实体类型（逗号分隔）
     */
    @ExcelProperty(value = "实体类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "逗=号分隔")
    private String entityTypes;

    /**
     * 关系类型（逗号分隔）
     */
    @ExcelProperty(value = "关系类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "逗=号分隔")
    private String relationTypes;

    /**
     * 错误信息
     */
    @ExcelProperty(value = "错误信息")
    private String errorMessage;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
