package org.ruoyi.service;

import java.util.Collection;
import java.util.List;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeAttachPicBo;
import org.ruoyi.domain.vo.KnowledgeAttachPicVo;

/**
 * 知识库附件图片列Service接口
 *
 * @author Albert
 * @date 2025-05-15
 */
public interface IKnowledgeAttachPicService {

  /**
   * 查询知识库附件图片列
   */
  KnowledgeAttachPicVo queryById(Long id);

  /**
   * 查询知识库附件图片列列表
   */
  TableDataInfo<KnowledgeAttachPicVo> queryPageList(KnowledgeAttachPicBo bo, PageQuery pageQuery);

  /**
   * 查询知识库附件图片列列表
   */
  List<KnowledgeAttachPicVo> queryList(KnowledgeAttachPicBo bo);

  /**
   * 新增知识库附件图片列
   */
  Boolean insertByBo(KnowledgeAttachPicBo bo);

  /**
   * 修改知识库附件图片列
   */
  Boolean updateByBo(KnowledgeAttachPicBo bo);

  /**
   * 校验并批量删除知识库附件图片列信息
   */
  Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
