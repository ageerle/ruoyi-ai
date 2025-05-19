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
import lombok.extern.slf4j.Slf4j;
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
import org.ruoyi.domain.KnowledgeAttachPic;
import org.ruoyi.domain.KnowledgeFragment;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.domain.PdfFileContentResult;
import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.bo.KnowledgeInfoUploadBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.mapper.KnowledgeAttachMapper;
import org.ruoyi.mapper.KnowledgeAttachPicMapper;
import org.ruoyi.mapper.KnowledgeFragmentMapper;
import org.ruoyi.mapper.KnowledgeInfoMapper;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.PdfImageExtractService;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.impl.PdfImageExtractServiceImpl;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.utils.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.ruoyi.system.service.ISysOssService;

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

//  private final PdfImageExtractService pdfImageExtractService;

  private final KnowledgeAttachPicMapper picMapper;

  private final DealFileService dealFileService;

  @Value("${pdf.extract.service.url}")
  private String serviceUrl;
  @Value("${pdf.extract.ai-api.url}")
  private String aiApiUrl;
  @Value("${pdf.extract.ai-api.key}")
  private String aiApiKey;

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
        vectorStoreService.createSchema(String.valueOf(knowledgeInfo.getId()),
            bo.getVectorModelName());
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
      vectorStoreService.removeByKid(String.valueOf(knowledgeInfoVo.getId()),
          knowledgeInfoVo.getVectorModelName());
    });
    // 删除附件和知识片段
    fragmentMapper.deleteByMap(map);
    List<KnowledgeAttachVo> knowledgeAttachVos = attachMapper.selectVoByMap(map);
    if (ObjectUtil.isNotEmpty(knowledgeAttachVos)) {
      Collection<Long> ossIds = knowledgeAttachVos.stream()
          .map(KnowledgeAttachVo::getOssId)
          .collect(Collectors.toList());
      //删除oss
      ossService.deleteWithValidByIds(ossIds, false);

      //删除图片oss
      List<KnowledgeAttachPic> knowledgeAttachPics = picMapper.selectList(
          new LambdaQueryWrapper<KnowledgeAttachPic>()
              .in(KnowledgeAttachPic::getKid,
                  knowledgeAttachVos.stream().map(KnowledgeAttachVo::getKid)
                      .collect(Collectors.toList()))
              .in(KnowledgeAttachPic::getAid,
                  knowledgeAttachVos.stream().map(KnowledgeAttachVo::getId)
                      .collect(Collectors.toList()))
      );
      if (ObjectUtil.isNotEmpty(knowledgeAttachPics)) {
        Collection<Long> tossIds = knowledgeAttachPics.stream()
            .map(KnowledgeAttachPic::getOssId)
            .collect(Collectors.toList());
        ossService.deleteWithValidByIds(tossIds, false);
        List<Long> collect = knowledgeAttachPics.stream().map(KnowledgeAttachPic::getId)
            .collect(Collectors.toList());
        picMapper.deleteByIds(collect);
      }
    }
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
   * 第一步 定时 拆解PDF文件中的图片
   */
  //@Scheduled(fixedDelay = 15000) // 每3秒执行一次
  public void dealKnowledgeAttachPic() throws Exception {
    //处理 拆解PDF文件中的图片的记录
    List<KnowledgeAttach> knowledgeAttaches = attachMapper.selectList(
        new LambdaQueryWrapper<KnowledgeAttach>()
            .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_10)
            .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_10)
            .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
    );
    log.info("===============拆解PDF文件中的图片 size = {}", knowledgeAttaches.size());
    if (ObjectUtil.isNotEmpty(knowledgeAttaches)) {
      for (KnowledgeAttach attachItem : knowledgeAttaches) {
        dealFileService.dealPicStatus(attachItem);
      }
    }
  }

  /**
   * 第二步 定时 解析图片内容
   */
  //@Scheduled(fixedDelay = 15000)
  public void dealKnowledgeAttachPicAnys() throws Exception {
    //获取未处理的图片记录
    List<KnowledgeAttachPic> knowledgeAttachPics = picMapper.selectList(
        new LambdaQueryWrapper<KnowledgeAttachPic>()
            .eq(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_10)
            .last("LIMIT 20")
    );
    if (ObjectUtil.isNotEmpty(knowledgeAttachPics)) {
      for (KnowledgeAttachPic picItem : knowledgeAttachPics) {
        dealFileService.dealPicAnysStatus(picItem);
      }
    }
  }
  /**
   * 第三步 定时 处理 附件上传后上传向量数据库
   */
  //@Scheduled(fixedDelay = 30000) // 每3秒执行一次
  public void dealKnowledgeAttachVector() throws Exception {
    //处理 需要上传向量数据库的记录
    List<KnowledgeAttach> knowledgeAttaches = attachMapper.selectList(
        new LambdaQueryWrapper<KnowledgeAttach>()
            .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
            .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
            .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
    );
    log.info("===============上传向量数据库 size = {}", knowledgeAttaches.size());
    if (ObjectUtil.isNotEmpty(knowledgeAttaches)) {
      for (KnowledgeAttach attachItem : knowledgeAttaches) {
        dealFileService.dealVectorStatus(attachItem);
      }
    }
  }
  /**
   * 第四步 定时 处理 失败数据
   */
  //@Scheduled(fixedDelay = 30 * 60 * 1000)
  public void dealKnowledge40Status() throws Exception {
      //拆解PDF失败 重新设置状态
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getPicStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_40));
      //将图片分析失败的数据 重新设置状态
      picMapper.update(new LambdaUpdateWrapper<KnowledgeAttachPic>()
          .set(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_40));
      //上传向量库失败 重新设置状态
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_40));
  }
    @Scheduled(fixedDelay = 180000) // 3分钟执行一次
}
