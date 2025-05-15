package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import org.ruoyi.core.domain.BaseEntity;

/**
 * 知识库附件图片列对象 knowledge_attach_pic
 *
 * @author Albert
 * @date 2025-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_attach_pic")
public class KnowledgeAttachPic  extends BaseEntity {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * 主键
   */
  @TableId(value = "id")
  private Long id;

  /**
   * 知识库id
   */
  private String kid;

  /**
   * 附件id
   */
  private String aid;

  /**
   * 文档名称
   */
  private String docName;

  /**
   * 文档类型
   */
  private String docType;

  /**
   * 文档内容
   */
  private String content;

  /**
   * 所在页数
   */
  private Integer pageNum;

  /**
   * 所在页index
   */
  private Integer indexNum;

  /**
   * 分析图片状态10未开始，20进行中，30已完成
   */
  private Integer picAnysStatus;

  /**
   * 对象存储主键
   */
  private Long ossId;

  /**
   * 备注
   */
  private String remark;


}
