package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.KnowledgeAttach;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 知识库附件业务对象 knowledge_attach
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeAttach.class, reverseConvertGenerate = false)
public class KnowledgeAttachBo extends BaseEntity {

  /**
   *
   */
  @NotNull(message = "不能为空", groups = {EditGroup.class})
  private Long id;

  /**
   * 知识库ID
   */
  @NotBlank(message = "知识库ID不能为空", groups = {AddGroup.class, EditGroup.class})
  private String kid;

  /**
   * 文档ID
   */
  @NotBlank(message = "文档ID不能为空", groups = {AddGroup.class, EditGroup.class})
  private String docId;

  /**
   * 文档名称
   */
  @NotBlank(message = "文档名称不能为空", groups = {AddGroup.class, EditGroup.class})
  private String docName;

  /**
   * 文档类型
   */
  @NotBlank(message = "文档类型不能为空", groups = {AddGroup.class, EditGroup.class})
  private String docType;

  /**
   * 文档内容
   */
  @NotBlank(message = "文档内容不能为空", groups = {AddGroup.class, EditGroup.class})
  private String content;

  /**
   * 备注
   */
  @NotBlank(message = "备注不能为空", groups = {AddGroup.class, EditGroup.class})
  private String remark;

  /**
   * 对象存储主键
   */
  @NotNull(message = "对象存储主键不能为空", groups = {AddGroup.class, EditGroup.class})
  private Long ossId;


  /**
   * 拆解图片状态10未开始，20进行中，30已完成
   */
  @NotNull(message = "拆解图片状态10未开始，20进行中，30已完成不能为空", groups = { AddGroup.class, EditGroup.class })
  private Integer picStatus;

  /**
   * 分析图片状态10未开始，20进行中，30已完成
   */
  @NotNull(message = "分析图片状态10未开始，20进行中，30已完成不能为空", groups = { AddGroup.class, EditGroup.class })
  private Integer picAnysStatus;

  /**
   * 写入向量数据库状态10未开始，20进行中，30已完成
   */
  @NotNull(message = "写入向量数据库状态10未开始，20进行中，30已完成不能为空", groups = { AddGroup.class, EditGroup.class })
  private Integer vectorStatus;


}
