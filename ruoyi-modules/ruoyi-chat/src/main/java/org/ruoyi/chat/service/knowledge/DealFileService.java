package org.ruoyi.chat.service.knowledge;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chain.loader.ResourceLoaderFactory;
import org.ruoyi.constant.DealStatus;
import org.ruoyi.domain.KnowledgeAttach;
import org.ruoyi.domain.KnowledgeAttachPic;
import org.ruoyi.domain.KnowledgeFragment;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.domain.PdfFileContentResult;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.mapper.KnowledgeAttachMapper;
import org.ruoyi.mapper.KnowledgeAttachPicMapper;
import org.ruoyi.mapper.KnowledgeFragmentMapper;
import org.ruoyi.mapper.KnowledgeInfoMapper;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.service.impl.PdfImageExtractServiceImpl;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.service.ISysOssService;
import org.ruoyi.utils.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Description:
 * @Date: 2025/5/15 下午4:29
 */
@Service
@RequiredArgsConstructor
public class DealFileService {
  private static final Logger log = LoggerFactory.getLogger(DealFileService.class);

  private final KnowledgeInfoMapper baseMapper;

  private final VectorStoreService vectorStoreService;

  private final ResourceLoaderFactory resourceLoaderFactory;

  private final KnowledgeFragmentMapper fragmentMapper;

  private final KnowledgeAttachMapper attachMapper;

  private final IChatModelService chatModelService;

  private final ISysOssService ossService;

//  private final PdfImageExtractService pdfImageExtractService;

  private final KnowledgeAttachPicMapper picMapper;

  @Value("${pdf.extract.service.url}")
  private String serviceUrl;
  @Value("${pdf.extract.ai-api.url}")
  private String aiApiUrl;
  @Value("${pdf.extract.ai-api.key}")
  private String aiApiKey;


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
      // 通过kid查询知识库信息
      KnowledgeInfoVo knowledgeInfoVo = baseMapper.selectVoOne(Wrappers.<KnowledgeInfo>lambdaQuery()
          .eq(KnowledgeInfo::getId, attachItem.getKid()));
      // 通过向量模型查询模型信息
      ChatModelVo chatModelVo = chatModelService.selectModelByName(
          knowledgeInfoVo.getEmbeddingModelName());

      StoreEmbeddingBo storeEmbeddingBo = new StoreEmbeddingBo();
      storeEmbeddingBo.setKid(attachItem.getKid());
      storeEmbeddingBo.setDocId(attachItem.getDocId());
      storeEmbeddingBo.setFids(fids);
      storeEmbeddingBo.setChunkList(chunkList);
      storeEmbeddingBo.setVectorModelName(knowledgeInfoVo.getVectorModelName());
      storeEmbeddingBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModelName());
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
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_40)
          .set(KnowledgeAttach::getRemark, attachItem.getRemark() + e.getMessage())
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getId, attachItem.getId()));
      throw new RuntimeException(e);
    }
  }

  @Async
  public void dealPicStatus(KnowledgeAttach attachItem) throws Exception {
    try {
      //锁定数据 更改picStatus 到进行中
      if (attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getPicStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getId, attachItem.getId())
      ) == 0) {
        return;
      }
      //获取附件
      if (ObjectUtil.isEmpty(attachItem.getOssId())) {
        log.error("==========OssId 为空，attachItem={}", attachItem);
        throw new Exception("OssId 为空");
      }
      //获取oss文件
      MultipartFile multipartFile = ossService.downloadByFile(attachItem.getOssId());
      //拆解出图片ZIP
      PdfImageExtractServiceImpl pdfImageExtractService = new PdfImageExtractServiceImpl(serviceUrl,
          aiApiUrl, aiApiKey);
      byte[] pngs = pdfImageExtractService.extractImages(multipartFile, "png", true);
      //解压zip，得到图片文件
      MultipartFile[] multipartFiles = ZipUtils.unzipToMultipartFiles(pngs);
      //上传文件到OSS，写入表
      for (MultipartFile file : multipartFiles) {
        //先查找是否有相同图片名称，先做删除
        List<KnowledgeAttachPic> knowledgeAttachPics = picMapper.selectList(
            new LambdaQueryWrapper<KnowledgeAttachPic>()
                .eq(KnowledgeAttachPic::getKid, attachItem.getKid())
                .eq(KnowledgeAttachPic::getAid, attachItem.getId())
                .eq(KnowledgeAttachPic::getDocName, file.getOriginalFilename())
        );
        if (ObjectUtil.isNotEmpty(knowledgeAttachPics)) {
          Collection<Long> ossIds = knowledgeAttachPics.stream()
              .map(KnowledgeAttachPic::getOssId)
              .collect(Collectors.toList());
          ossService.deleteWithValidByIds(ossIds, false);
          List<Long> collect = knowledgeAttachPics.stream().map(KnowledgeAttachPic::getId)
              .collect(Collectors.toList());
          picMapper.deleteByIds(collect);
        }

        SysOssVo upload = ossService.upload(file);
        KnowledgeAttachPic entity = new KnowledgeAttachPic();
        entity.setKid(attachItem.getKid());
        entity.setAid(String.valueOf(attachItem.getId()));
        entity.setDocName(file.getOriginalFilename());
        entity.setDocType(
            file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
        entity.setOssId(upload.getOssId());
        int[] ints = extractPageNumbers(file.getOriginalFilename());
        if (ObjectUtil.isNotEmpty(ints)) {
          assert ints != null;
          if (ints.length == 2) {
            entity.setPageNum(ints[0]);
            entity.setIndexNum(ints[1]);
          }
        }
        picMapper.insert(entity);
      }

      //设置处理完成
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getId, attachItem.getId()));
    } catch (Exception e) {
      //设置处理失败
      attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
          .set(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_40)
          .set(KnowledgeAttach::getRemark, attachItem.getRemark() + e.getMessage())
          .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttach::getId, attachItem.getId()));
      throw new RuntimeException(e);
    }

  }


  @Async
  public void dealPicAnysStatus(KnowledgeAttachPic picItem) throws Exception {
    try {
      //锁定数据 更改 getPicAnysStatus 到进行中
      if (picMapper.update(new LambdaUpdateWrapper<KnowledgeAttachPic>()
          .set(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttachPic::getId, picItem.getId())
      ) == 0) {
        return;
      }
      SysOssVo ossVo = ossService.getById(picItem.getOssId());
      if (ObjectUtil.isNotEmpty(ossVo)) {
        String fileStr = ossService.downloadByByte(picItem.getOssId());
        //调用第三方 分析图片内容
        PdfImageExtractServiceImpl pdfImageExtractService = new PdfImageExtractServiceImpl(
            serviceUrl,
            aiApiUrl, aiApiKey);
        List<PdfFileContentResult> pdfFileContentResults = pdfImageExtractService.dealFileContent(
            new String[]{fileStr});
        if (ObjectUtil.isNotEmpty(pdfFileContentResults)) {
          for (PdfFileContentResult resultItem : pdfFileContentResults) {
            //图片解析内容回写到pic表
            picMapper.update(new LambdaUpdateWrapper<KnowledgeAttachPic>()
                .set(KnowledgeAttachPic::getContent, parseContent(resultItem.getContent()))
                .set(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_30)
                .eq(KnowledgeAttachPic::getId, picItem.getId()));
            //将图片解析内容 写入段落表 fragment
            KnowledgeAttachVo knowledgeAttachVo = attachMapper.selectVoById(picItem.getAid());
            if (ObjectUtil.isNotEmpty(knowledgeAttachVo)) {
              String fid = RandomUtil.randomString(10);
              KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
              knowledgeFragment.setKid(knowledgeAttachVo.getKid());
              knowledgeFragment.setDocId(knowledgeAttachVo.getDocId());
              knowledgeFragment.setFid(fid);
              knowledgeFragment.setIdx(0);
              knowledgeFragment.setContent(parseContent(resultItem.getContent()));
              knowledgeFragment.setCreateTime(new Date());
              fragmentMapper.insert(knowledgeFragment);

              //更新attach表，需要所有图片都处理完毕
              // 查询非30状态（完成状态）的记录数量
              long nonStatus30Count = picMapper.selectCount(
                  new LambdaQueryWrapper<KnowledgeAttachPic>()
                      .ne(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_30)
                      .eq(KnowledgeAttachPic::getAid, picItem.getAid())
              );
              if (nonStatus30Count == 0) {
                // 执行表更新操作
                attachMapper.update(new LambdaUpdateWrapper<KnowledgeAttach>()
                    .set(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_30)
                    .eq(KnowledgeAttach::getPicStatus, DealStatus.STATUS_30)
                    .eq(KnowledgeAttach::getPicAnysStatus, DealStatus.STATUS_10)
                    .eq(KnowledgeAttach::getVectorStatus, DealStatus.STATUS_10)
                    .eq(KnowledgeAttach::getId, picItem.getAid()));
              }
            }
          }
        }
      }
    } catch (Exception e) {
      //失败
      picMapper.update(new LambdaUpdateWrapper<KnowledgeAttachPic>()
          .set(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_40)
          .set(KnowledgeAttachPic::getRemark, picItem.getRemark() + e.getMessage())
          .eq(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_20)
          .eq(KnowledgeAttachPic::getId, picItem.getId()));
      throw new RuntimeException(e);
    } finally {
      //将图片分析失败的数据 重新设置状态
      picMapper.update(new LambdaUpdateWrapper<KnowledgeAttachPic>()
          .set(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_10)
          .eq(KnowledgeAttachPic::getPicAnysStatus, DealStatus.STATUS_40));
    }
  }


  /**
   * 从文件名中提取page后面的两个数字
   *
   * @param fileName 文件名
   * @return 包含两个数字的数组，如果未找到则返回null
   */
  public static int[] extractPageNumbers(String fileName) {
    // 查找"page_"的位置
    int pageIndex = fileName.indexOf("page_");

    if (pageIndex == -1) {
      return null;
    }

    // 从"page_"后开始截取
    String afterPage = fileName.substring(pageIndex + 5);

    // 按下划线分割
    String[] parts = afterPage.split("_");

    if (parts.length >= 2) {
      try {
        // 提取两个数字
        int firstNumber = Integer.parseInt(parts[0]);

        // 对于第二个数字，需要去掉可能的文件扩展名
        String secondPart = parts[1];
        int dotIndex = secondPart.indexOf(".");
        if (dotIndex != -1) {
          secondPart = secondPart.substring(0, dotIndex);
        }

        int secondNumber = Integer.parseInt(secondPart);

        return new int[]{firstNumber, secondNumber};
      } catch (NumberFormatException e) {
        return null;
      }
    }

    return null;
  }

  public static String parseContent(String jsonString) {
    try {
      // 创建ObjectMapper实例
      ObjectMapper objectMapper = new ObjectMapper();

      // 解析JSON字符串
      JsonNode rootNode = objectMapper.readTree(jsonString);

      // 获取choices数组的第一个元素
      JsonNode choicesNode = rootNode.get("choices");
      if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
        // 获取第一个choice
        JsonNode firstChoice = choicesNode.get(0);

        // 获取message节点
        JsonNode messageNode = firstChoice.get("message");
        if (messageNode != null) {
          // 获取content字段的值
          JsonNode contentNode = messageNode.get("content");
          if (contentNode != null) {
            return contentNode.asText();
          }
        }
      }

      return "无法找到content内容";
    } catch (Exception e) {
      e.printStackTrace();
      return "解析JSON时发生错误: " + e.getMessage();
    }
  }


}
