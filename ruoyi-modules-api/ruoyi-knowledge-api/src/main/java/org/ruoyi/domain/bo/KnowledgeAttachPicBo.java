package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.KnowledgeAttachPic;

/**
 * 知识库附件图片列业务对象 knowledge_attach_pic
 *
 * @author Albert
 * @date 2025-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeAttachPic.class, reverseConvertGenerate = false)
public class KnowledgeAttachPicBo extends BaseEntity {

  /**
   * 主键
   */
  @NotNull(message = "主键不能为空", groups = {EditGroup.class})
  private Long id;

  /**
   * 知识库id
   */
  @NotBlank(message = "知识库id不能为空", groups = {AddGroup.class, EditGroup.class})
  private String kid;

  /**
   * 附件id
   */
  @NotBlank(message = "附件id不能为空", groups = {AddGroup.class, EditGroup.class})
  private String aid;

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
   * 所在页数
   */
  @NotNull(message = "所在页数不能为空", groups = {AddGroup.class, EditGroup.class})
  private Integer pageNum;

  /**
   * 所在页index
   */
  @NotNull(message = "所在页index不能为空", groups = {AddGroup.class, EditGroup.class})
  private Integer indexNum;

  /**
   * 分析图片状态10未开始，20进行中，30已完成
   */
  @NotNull(message = "分析图片状态10未开始，20进行中，30已完成不能为空", groups = {AddGroup.class,
      EditGroup.class})
  private Integer picAnysStatus;

  /**
   * 对象存储主键
   */
  @NotNull(message = "对象存储主键不能为空", groups = {AddGroup.class, EditGroup.class})
  private Long ossId;

  /**
   * 备注
   */
  @NotBlank(message = "备注不能为空", groups = {AddGroup.class, EditGroup.class})
  private String remark;

}
