package org.ruoyi.chat.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chain.loader.ResourceLoader;
import org.ruoyi.chain.loader.ResourceLoaderFactory;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.constant.DealStatus;
import org.ruoyi.constant.FileType;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.ChatModel;
import org.ruoyi.domain.KnowledgeAttach;
import org.ruoyi.domain.KnowledgeFragment;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.bo.KnowledgeInfoUploadBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.mapper.KnowledgeAttachMapper;
import org.ruoyi.mapper.KnowledgeFragmentMapper;
import org.ruoyi.mapper.KnowledgeInfoMapper;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.service.ISysOssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.*;

/**
 * 知识库Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeInfoServiceImpl implements IKnowledgeInfoService {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeInfoServiceImpl.class);
  private final KnowledgeInfoMapper baseMapper;

  private final VectorStoreService vectorStoreService;

  private final ResourceLoaderFactory resourceLoaderFactory;

  private final KnowledgeFragmentMapper fragmentMapper;

  private final KnowledgeAttachMapper attachMapper;

  private final IChatModelService chatModelService;

  private final ISysOssService ossService;

  /**
   * 查询知识库
   */
  @Override
  public KnowledgeInfoVo queryById(Long id) {
    return baseMapper.selectVoById(id);
  }

  /**
   * 查询知识库列表
   */
  @Override
  public TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery) {
    LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
    Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    return TableDataInfo.build(result);
  }

  /**
   * 查询知识库列表
   */
  @Override
  public List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo) {
    LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
    return baseMapper.selectVoList(lqw);
  }

  private LambdaQueryWrapper<KnowledgeInfo> buildQueryWrapper(KnowledgeInfoBo bo) {
    Map<String, Object> params = bo.getParams();
    LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
    lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeInfo::getKid, bo.getKid());
    lqw.eq(bo.getUid() != null, KnowledgeInfo::getUid, bo.getUid());
    lqw.like(StringUtils.isNotBlank(bo.getKname()), KnowledgeInfo::getKname, bo.getKname());
    lqw.eq(bo.getShare() != null, KnowledgeInfo::getShare, bo.getShare());
    lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription,
        bo.getDescription());
    lqw.eq(StringUtils.isNotBlank(bo.getKnowledgeSeparator()), KnowledgeInfo::getKnowledgeSeparator,
        bo.getKnowledgeSeparator());
    lqw.eq(StringUtils.isNotBlank(bo.getQuestionSeparator()), KnowledgeInfo::getQuestionSeparator,
        bo.getQuestionSeparator());
    lqw.eq(bo.getOverlapChar() != null, KnowledgeInfo::getOverlapChar, bo.getOverlapChar());
    lqw.eq(bo.getRetrieveLimit() != null, KnowledgeInfo::getRetrieveLimit, bo.getRetrieveLimit());
    lqw.eq(bo.getTextBlockSize() != null, KnowledgeInfo::getTextBlockSize, bo.getTextBlockSize());
    lqw.eq(StringUtils.isNotBlank(bo.getVector()), KnowledgeInfo::getVector, bo.getVector());
    lqw.eq(StringUtils.isNotBlank(bo.getVectorModel()), KnowledgeInfo::getVectorModel,
        bo.getVectorModel());
    return lqw;
  }

  /**
   * 新增知识库
   */
  @Override
  public Boolean insertByBo(KnowledgeInfoBo bo) {
    KnowledgeInfo add = MapstructUtils.convert(bo, KnowledgeInfo.class);
    validEntityBeforeSave(add);
    boolean flag = baseMapper.insert(add) > 0;
    if (flag) {
      bo.setId(add.getId());
    }
    return flag;
  }

  /**
   * 修改知识库
   */
  @Override
  public Boolean updateByBo(KnowledgeInfoBo bo) {
    KnowledgeInfo update = MapstructUtils.convert(bo, KnowledgeInfo.class);
    validEntityBeforeSave(update);
    return baseMapper.updateById(update) > 0;
  }

  /**
   * 保存前的数据校验
   */
  private void validEntityBeforeSave(KnowledgeInfo entity) {
    //TODO 做一些数据校验,如唯一约束
  }

  /**
   * 批量删除知识库
   */
  @Override
  public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
    if (isValid) {
      //TODO 做一些业务上的校验,判断是否需要校验
    }
    return baseMapper.deleteBatchIds(ids) > 0;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void saveOne(KnowledgeInfoBo bo) {
    KnowledgeInfo knowledgeInfo = MapstructUtils.convert(bo, KnowledgeInfo.class);
    if (StringUtils.isBlank(bo.getKid())) {
      String kid = RandomUtil.randomString(10);
      if (knowledgeInfo != null) {
        knowledgeInfo.setKid(kid);
        knowledgeInfo.setUid(LoginHelper.getLoginUser().getUserId());
      }
      baseMapper.insert(knowledgeInfo);
      if (knowledgeInfo != null) {
        vectorStoreService.createSchema(String.valueOf(knowledgeInfo.getId()), bo.getVector());
      }
    } else {
      baseMapper.updateById(knowledgeInfo);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeKnowledge(String id) {
    Map<String, Object> map = new HashMap<>();
    map.put("kid", id);
    List<KnowledgeInfoVo> knowledgeInfoList = baseMapper.selectVoByMap(map);
    check(knowledgeInfoList);
    // 删除向量库信息
    knowledgeInfoList.forEach(knowledgeInfoVo -> {
      vectorStoreService.removeByKid(String.valueOf(knowledgeInfoVo.getId()));
    });
    // 删除附件和知识片段
    fragmentMapper.deleteByMap(map);
    attachMapper.deleteByMap(map);
    // 删除知识库
    baseMapper.deleteByMap(map);
  }

  @Override
  public void upload(KnowledgeInfoUploadBo bo) {
    storeContent(bo.getFile(), bo.getKid());
  }

  public void storeContent(MultipartFile file, String kid) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be null or empty");
    }

    SysOssVo uploadDto = null;

    String fileName = file.getOriginalFilename();
    List<String> chunkList = new ArrayList<>();
    KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
    knowledgeAttach.setKid(kid);
    String docId = RandomUtil.randomString(10);
    knowledgeAttach.setDocId(docId);
    knowledgeAttach.setDocName(fileName);
    knowledgeAttach.setDocType(fileName.substring(fileName.lastIndexOf(".") + 1));
    String content = "";
    ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(
        knowledgeAttach.getDocType());
    List<String> fids = new ArrayList<>();
    try {
      content = resourceLoader.getContent(file.getInputStream());
      chunkList = resourceLoader.getChunkList(content, kid);
      List<KnowledgeFragment> knowledgeFragmentList = new ArrayList<>();
      if (CollUtil.isNotEmpty(chunkList)) {
        // Upload file to OSS
        uploadDto = ossService.upload(file);

        for (int i = 0; i < chunkList.size(); i++) {
          String fid = RandomUtil.randomString(10);
          fids.add(fid);
          KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
          knowledgeFragment.setKid(kid);
          knowledgeFragment.setDocId(docId);
          knowledgeFragment.setFid(fid);
          knowledgeFragment.setIdx(i);
          knowledgeFragment.setContent(chunkList.get(i));
          knowledgeFragment.setCreateTime(new Date());
          knowledgeFragmentList.add(knowledgeFragment);
        }
      }
      fragmentMapper.insertBatch(knowledgeFragmentList);
    } catch (IOException e) {
      log.error("保存知识库信息失败！{}", e.getMessage());
    }
    knowledgeAttach.setContent(content);
    knowledgeAttach.setCreateTime(new Date());

    if (ObjectUtil.isNotEmpty(uploadDto) && ObjectUtil.isNotEmpty(uploadDto.getOssId())) {
      knowledgeAttach.setOssId(uploadDto.getOssId());
      //只有pdf文件 才需要拆解图片和分析图片内容
      if (FileType.PDF.equals(knowledgeAttach.getDocType())) {
        knowledgeAttach.setPicStatus(DealStatus.STATUS_10);
        knowledgeAttach.setPicAnysStatus(DealStatus.STATUS_10);
      } else {
        knowledgeAttach.setPicStatus(DealStatus.STATUS_30);
        knowledgeAttach.setPicAnysStatus(DealStatus.STATUS_30);
      }
      //所有文件上传后，都需要同步到向量数据库
      knowledgeAttach.setVectorStatus(DealStatus.STATUS_10);
    }

    attachMapper.insert(knowledgeAttach);
  }


  /**
   * 检查用户是否有删除知识库权限
   *
   * @param knowledgeInfoList 知识库列表
   */
  public void check(List<KnowledgeInfoVo> knowledgeInfoList) {
    LoginUser loginUser = LoginHelper.getLoginUser();
    for (KnowledgeInfoVo knowledgeInfoVo : knowledgeInfoList) {
      if (!knowledgeInfoVo.getUid().equals(loginUser.getUserId())) {
        throw new SecurityException("权限不足");
      }
    }
  }

  /**
   * 定时 处理 附件上传后上传向量数据库和PDF文件图片拆解和分析内容
   */
  @Scheduled(fixedDelay = 3000) // 每3秒执行一次
  public void dealKnowledgeAttach() throws Exception {
    //处理 需要上传向量数据库的记录
    List<KnowledgeAttach> knowledgeAttaches = attachMapper.selectList(
        new LambdaQueryWrapper<KnowledgeAttach>()
            .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
            .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
            .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
    );
    if (ObjectUtil.isNotEmpty(knowledgeAttaches)) {
      for (KnowledgeAttach attachItem : knowledgeAttaches) {
        this.dealVectorStatus(attachItem);
      }
    }
  }

  @Async
  public void dealVectorStatus(KnowledgeAttach attachItem) throws Exception {
    try {
      //锁定数据 更改VectorStatus 到进行中
      if (attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getId, attachItem.getId())
      ) == 0) {
        return;
      }
      // 通过kid查询知识库信息
      KnowledgeInfoVo knowledgeInfoVo = baseMapper.selectVoOne(Wrappers.<KnowledgeInfo>lambdaQuery()
          .eq(KnowledgeInfo::getKid, attachItem.getKid()));

      // 通过向量模型查询模型信息
      ChatModelVo chatModelVo = chatModelService.selectModelByName(
          knowledgeInfoVo.getVectorModel());

      List<KnowledgeFragment> knowledgeFragments = fragmentMapper.selectList(
          new LambdaQueryWrapper<KnowledgeFragment>()
              .eq(KnowledgeFragment::getKid, attachItem.getKid())
              .eq(KnowledgeFragment::getDocId, attachItem.getDocId())
      );
      if (ObjectUtil.isEmpty(knowledgeFragments)) {
        throw new Exception("文件段落为空");
      }
      List<String> fids = knowledgeFragments.stream()
          .map(KnowledgeFragment::getFid)
          .collect(Collectors.toList());
      if (ObjectUtil.isEmpty(fids)) {
        throw new Exception("fids 为空");
      }
      List<String> chunkList = knowledgeFragments.stream()
          .map(KnowledgeFragment::getContent)
          .collect(Collectors.toList());

      if (ObjectUtil.isEmpty(chunkList)) {
        throw new Exception("chunkList 为空");
      }
      StoreEmbeddingBo storeEmbeddingBo = new StoreEmbeddingBo();
      storeEmbeddingBo.setKid(attachItem.getKid());
      storeEmbeddingBo.setDocId(attachItem.getDocId());
      storeEmbeddingBo.setFids(fids);
      storeEmbeddingBo.setChunkList(chunkList);
      storeEmbeddingBo.setModelName(knowledgeInfoVo.getVectorModel());
      storeEmbeddingBo.setApiKey(chatModelVo.getApiKey());
      storeEmbeddingBo.setBaseUrl(chatModelVo.getApiHost());
      vectorStoreService.storeEmbeddings(storeEmbeddingBo);

      //设置处理完成
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getId, attachItem.getId()));
    } catch (Exception e) {
      //设置处理失败
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getId, attachItem.getId()));
      throw new RuntimeException(e);
    }
  }

}
