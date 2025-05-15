package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import org.ruoyi.domain.KnowledgeAttachPic;


/**
 * 知识库附件图片列视图对象 knowledge_attach_pic
 *
 * @author Albert
 * @date 2025-05-15
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeAttachPic.class)
public class KnowledgeAttachPicVo implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * 主键
   */
  @ExcelProperty(value = "主键")
  private Long id;

  /**
   * 知识库id
   */
  @ExcelProperty(value = "知识库id")
  private String kid;

  /**
   * 附件id
   */
  @ExcelProperty(value = "附件id")
  private String aid;

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
   * 所在页数
   */
  @ExcelProperty(value = "所在页数")
  private Integer pageNum;

  /**
   * 所在页index
   */
  @ExcelProperty(value = "所在页index")
  private Integer indexNum;

  /**
   * 分析图片状态10未开始，20进行中，30已完成
   */
  @ExcelProperty(value = "分析图片状态10未开始，20进行中，30已完成")
  private Integer picAnysStatus;

  /**
   * 对象存储主键
   */
  @ExcelProperty(value = "对象存储主键")
  private Long ossId;

  /**
   * 备注
   */
  @ExcelProperty(value = "备注")
  private String remark;

}
