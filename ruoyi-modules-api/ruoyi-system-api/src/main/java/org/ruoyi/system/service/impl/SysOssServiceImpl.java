package org.ruoyi.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CacheNames;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.common.oss.core.OssClient;
import org.ruoyi.common.oss.entity.UploadResult;
import org.ruoyi.common.oss.enumd.AccessPolicyType;
import org.ruoyi.common.oss.factory.OssFactory;
import org.ruoyi.system.domain.SysOss;
import org.ruoyi.system.domain.bo.SysOssBo;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.mapper.SysOssMapper;
import org.ruoyi.system.service.ISysOssService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件上传 服务层实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysOssServiceImpl implements ISysOssService, OssService {

  private final SysOssMapper baseMapper;

  @Override
  public TableDataInfo<SysOssVo> queryPageList(SysOssBo bo, PageQuery pageQuery) {
    LambdaQueryWrapper<SysOss> lqw = buildQueryWrapper(bo);
    Page<SysOssVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    List<SysOssVo> filterResult = StreamUtils.toList(result.getRecords(), this::matchingUrl);
    result.setRecords(filterResult);
    return TableDataInfo.build(result);
  }

  @Override
  public List<SysOssVo> listByIds(Collection<Long> ossIds) {
    List<SysOssVo> list = new ArrayList<>();
    for (Long id : ossIds) {
      SysOssVo vo = SpringUtils.getAopProxy(this).getById(id);
      if (ObjectUtil.isNotNull(vo)) {
        list.add(this.matchingUrl(vo));
      }
    }
    return list;
  }

  @Override
  public String selectUrlByIds(String ossIds) {
    List<String> list = new ArrayList<>();
    for (Long id : StringUtils.splitTo(ossIds, Convert::toLong)) {
      SysOssVo vo = SpringUtils.getAopProxy(this).getById(id);
      if (ObjectUtil.isNotNull(vo)) {
        list.add(this.matchingUrl(vo).getUrl());
      }
    }
    return String.join(StringUtils.SEPARATOR, list);
  }

  private LambdaQueryWrapper<SysOss> buildQueryWrapper(SysOssBo bo) {
    Map<String, Object> params = bo.getParams();
    LambdaQueryWrapper<SysOss> lqw = Wrappers.lambdaQuery();
    lqw.like(StringUtils.isNotBlank(bo.getFileName()), SysOss::getFileName, bo.getFileName());
    lqw.like(StringUtils.isNotBlank(bo.getOriginalName()), SysOss::getOriginalName,
        bo.getOriginalName());
    lqw.eq(StringUtils.isNotBlank(bo.getFileSuffix()), SysOss::getFileSuffix, bo.getFileSuffix());
    lqw.eq(StringUtils.isNotBlank(bo.getUrl()), SysOss::getUrl, bo.getUrl());
    lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
        SysOss::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
    lqw.eq(ObjectUtil.isNotNull(bo.getCreateBy()), SysOss::getCreateBy, bo.getCreateBy());
    lqw.eq(StringUtils.isNotBlank(bo.getService()), SysOss::getService, bo.getService());
    return lqw;
  }

  @Cacheable(cacheNames = CacheNames.SYS_OSS, key = "#ossId")
  @Override
  public SysOssVo getById(Long ossId) {
    return baseMapper.selectVoById(ossId);
  }

  @Override
  public void download(Long ossId, HttpServletResponse response) throws IOException {
    SysOssVo sysOss = SpringUtils.getAopProxy(this).getById(ossId);
    if (ObjectUtil.isNull(sysOss)) {
      throw new ServiceException("文件数据不存在!");
    }
    FileUtils.setAttachmentResponseHeader(response, sysOss.getOriginalName());
    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
    OssClient storage = OssFactory.instance();
    try (InputStream inputStream = storage.getObjectContent(sysOss.getUrl())) {
      int available = inputStream.available();
      IoUtil.copy(inputStream, response.getOutputStream(), available);
      response.setContentLength(available);
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    }
  }
  @Override
  public String downloadByByte(Long ossId) throws IOException {
    SysOssVo sysOss = SpringUtils.getAopProxy(this).getById(ossId);
    if (ObjectUtil.isNull(sysOss)) {
      throw new ServiceException("文件数据不存在!");
    }

    OssClient storage = OssFactory.instance();
    try (InputStream inputStream = storage.getObjectContent(sysOss.getUrl())) {
      // 读取输入流中的所有字节
      byte[] bytes = IoUtil.readBytes(inputStream);
      // 将字节数组转换为Base64编码的字符串
      return Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    }
  }

  @Override
  public MultipartFile downloadByFile(Long ossId) throws IOException {
    SysOssVo sysOss = SpringUtils.getAopProxy(this).getById(ossId);
    if (ObjectUtil.isNull(sysOss)) {
      throw new ServiceException("文件数据不存在!");
    }

    OssClient storage = OssFactory.instance();
    try (InputStream inputStream = storage.getObjectContent(sysOss.getUrl())) {
      byte[] content = IoUtil.readBytes(inputStream);
      return new MockMultipartFile(
          sysOss.getFileName(),
          sysOss.getOriginalName(),
          MediaType.APPLICATION_OCTET_STREAM_VALUE,
          content
      );
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    }
  }

  @Override
  public SysOssVo upload(MultipartFile file) {
    String originalfileName = file.getOriginalFilename();
    String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."),
        originalfileName.length());
    OssClient storage = OssFactory.instance();
    UploadResult uploadResult;
    try {
      uploadResult = storage.uploadSuffix(file.getBytes(), suffix, file.getContentType());
    } catch (IOException e) {
      throw new ServiceException(e.getMessage());
    }
    // 保存文件信息
    SysOss oss = new SysOss();
    oss.setUrl(uploadResult.getUrl());
    oss.setFileSuffix(suffix);
    oss.setFileName(uploadResult.getFilename());
    oss.setOriginalName(originalfileName);
    oss.setService(storage.getConfigKey());
    baseMapper.insert(oss);
    SysOssVo sysOssVo = MapstructUtils.convert(oss, SysOssVo.class);
    return this.matchingUrl(sysOssVo);
  }

  @Override
  public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
    if (isValid) {
      // 做一些业务上的校验,判断是否需要校验
    }
    List<SysOss> list = baseMapper.selectBatchIds(ids);
    for (SysOss sysOss : list) {
      OssClient storage = OssFactory.instance(sysOss.getService());
      storage.delete(sysOss.getUrl());
    }
    return baseMapper.deleteBatchIds(ids) > 0;
  }

  /**
   * 匹配Url
   *
   * @param oss OSS对象
   * @return oss 匹配Url的OSS对象
   */
  private SysOssVo matchingUrl(SysOssVo oss) {
    OssClient storage = OssFactory.instance(oss.getService());
    // 仅修改桶类型为 private 的URL，临时URL时长为120s
    if (AccessPolicyType.PRIVATE == storage.getAccessPolicy()) {
      oss.setUrl(storage.getPrivateUrl(oss.getFileName(), 120));
    }
    return oss;
  }
}
