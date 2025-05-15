package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.KnowledgeAttachPic;
import org.ruoyi.domain.bo.KnowledgeAttachPicBo;
import org.ruoyi.domain.vo.KnowledgeAttachPicVo;
import org.ruoyi.mapper.KnowledgeAttachPicMapper;
import org.ruoyi.service.IKnowledgeAttachPicService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识库附件图片列Service业务层处理
 *
 * @author ageerle
 * @date 2025-05-15
 */
@RequiredArgsConstructor
@Service
public class KnowledgeAttachPicServiceImpl implements IKnowledgeAttachPicService {

  private final KnowledgeAttachPicMapper baseMapper;

  /**
   * 查询知识库附件图片列
   */
  @Override
  public KnowledgeAttachPicVo queryById(Long id) {
    return baseMapper.selectVoById(id);
  }

  /**
   * 查询知识库附件图片列列表
   */
  @Override
  public TableDataInfo<KnowledgeAttachPicVo> queryPageList(KnowledgeAttachPicBo bo,
      PageQuery pageQuery) {
    LambdaQueryWrapper<KnowledgeAttachPic> lqw = buildQueryWrapper(bo);
    Page<KnowledgeAttachPicVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    return TableDataInfo.build(result);
  }

  /**
   * 查询知识库附件图片列列表
   */
  @Override
  public List<KnowledgeAttachPicVo> queryList(KnowledgeAttachPicBo bo) {
    LambdaQueryWrapper<KnowledgeAttachPic> lqw = buildQueryWrapper(bo);
    return baseMapper.selectVoList(lqw);
  }

  private LambdaQueryWrapper<KnowledgeAttachPic> buildQueryWrapper(KnowledgeAttachPicBo bo) {
    Map<String, Object> params = bo.getParams();
    LambdaQueryWrapper<KnowledgeAttachPic> lqw = Wrappers.lambdaQuery();
    lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeAttachPic::getKid, bo.getKid());
    lqw.eq(StringUtils.isNotBlank(bo.getAid()), KnowledgeAttachPic::getAid, bo.getAid());
    lqw.like(StringUtils.isNotBlank(bo.getDocName()), KnowledgeAttachPic::getDocName,
        bo.getDocName());
    lqw.eq(StringUtils.isNotBlank(bo.getDocType()), KnowledgeAttachPic::getDocType,
        bo.getDocType());
    lqw.eq(StringUtils.isNotBlank(bo.getContent()), KnowledgeAttachPic::getContent,
        bo.getContent());
    lqw.eq(bo.getPageNum() != null, KnowledgeAttachPic::getPageNum, bo.getPageNum());
    lqw.eq(bo.getIndexNum() != null, KnowledgeAttachPic::getIndexNum, bo.getIndexNum());
    lqw.eq(bo.getPicAnysStatus() != null, KnowledgeAttachPic::getPicAnysStatus,
        bo.getPicAnysStatus());
    lqw.eq(bo.getOssId() != null, KnowledgeAttachPic::getOssId, bo.getOssId());
    return lqw;
  }

  /**
   * 新增知识库附件图片列
   */
  @Override
  public Boolean insertByBo(KnowledgeAttachPicBo bo) {
    KnowledgeAttachPic add = MapstructUtils.convert(bo, KnowledgeAttachPic.class);
    validEntityBeforeSave(add);
    boolean flag = baseMapper.insert(add) > 0;
    if (flag) {
      bo.setId(add.getId());
    }
    return flag;
  }

  /**
   * 修改知识库附件图片列
   */
  @Override
  public Boolean updateByBo(KnowledgeAttachPicBo bo) {
    KnowledgeAttachPic update = MapstructUtils.convert(bo, KnowledgeAttachPic.class);
    validEntityBeforeSave(update);
    return baseMapper.updateById(update) > 0;
  }

  /**
   * 保存前的数据校验
   */
  private void validEntityBeforeSave(KnowledgeAttachPic entity) {
    //TODO 做一些数据校验,如唯一约束
  }

  /**
   * 批量删除知识库附件图片列
   */
  @Override
  public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
    if (isValid) {
      //TODO 做一些业务上的校验,判断是否需要校验
    }
    return baseMapper.deleteBatchIds(ids) > 0;
  }
}

