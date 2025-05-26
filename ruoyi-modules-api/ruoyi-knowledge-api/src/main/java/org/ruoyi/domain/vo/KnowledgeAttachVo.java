package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.KnowledgeAttach;

import java.io.Serial;
import java.io.Serializable;


/**
 * 知识库附件视图对象 knowledge_attach
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeAttach.class)
public class KnowledgeAttachVo implements Serializable {

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
   * 文档名称
   */
  @ExcelProperty(value = "文档名称")
  private String docName;

  /**
   * 文档类型
   */
  @ExcelProperty(value = "文档类型")
  private String docType;

  /**
   * 文档内容
   */
  @ExcelProperty(value = "文档内容")
  private String content;

  /**
   * 备注
   */
  @ExcelProperty(value = "备注")
  private String remark;

  /**
   * 对象存储主键
   */
  @ExcelProperty(value = "对象存储主键")
  private Long ossId;



  /**
   * 拆解图片状态10未开始，20进行中，30已完成
   */
  @ExcelProperty(value = "拆解图片状态10未开始，20进行中，30已完成")
  private Integer picStatus;

  /**
   * 分析图片状态10未开始，20进行中，30已完成
   */
  @ExcelProperty(value = "分析图片状态10未开始，20进行中，30已完成")
  private Integer picAnysStatus;

  /**
   * 写入向量数据库状态10未开始，20进行中，30已完成
   */
  @ExcelProperty(value = "写入向量数据库状态10未开始，20进行中，30已完成")
  private Integer vectorStatus;
}
