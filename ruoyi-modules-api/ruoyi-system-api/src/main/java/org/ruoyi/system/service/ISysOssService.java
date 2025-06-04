package org.ruoyi.system.service;

import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysOssBo;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * 文件上传 服务层
 *
 * @author Lion Li
 */
public interface ISysOssService {

  TableDataInfo<SysOssVo> queryPageList(SysOssBo sysOss, PageQuery pageQuery);

  List<SysOssVo> listByIds(Collection<Long> ossIds);

  SysOssVo getById(Long ossId);

  SysOssVo upload(MultipartFile file);

  void download(Long ossId, HttpServletResponse response) throws IOException;

  MultipartFile downloadByFile(Long ossId) throws IOException;

  String downloadByByte(Long ossId) throws IOException;

  String downloadToTempPath(Long ossId) throws IOException;

  Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

  /**
   * 根据文件路径删除文件
   *
   * @param filePath 文件路径
   * @return 是否删除成功
   */
  boolean deleteFile(String filePath);
}
