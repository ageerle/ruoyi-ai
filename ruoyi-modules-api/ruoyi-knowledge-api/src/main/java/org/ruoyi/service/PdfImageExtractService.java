package org.ruoyi.service;

import java.io.IOException;
import java.util.List;
import org.ruoyi.domain.PdfFileContentResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF图片提取服务接口
 */
public interface PdfImageExtractService {

  /**
   * 从PDF文件中提取图片
   *
   * @param pdfFile PDF文件
   * @param imageFormat 输出图片格式 (png, jpeg, gif)
   * @param allowDuplicates 是否允许重复图片
   * @return 包含提取图片的ZIP文件的字节数组
   * @throws IOException 如果文件处理过程中发生错误
   */
  byte[] extractImages(MultipartFile pdfFile, String imageFormat, boolean allowDuplicates)
      throws IOException;

  /**
   * 处理文件内容
   *
   * @param unzip Base64编码的图片数组
   * @return 文件内容结果列表
   * @throws IOException 如果API调用过程中发生错误
   */
  List<PdfFileContentResult> dealFileContent(String[] unzip) throws IOException;

  /**
   * 提取PDF中的图片并调用gpt-4o-mini,识别图片内容并返回
   * @param file
   * @return
   * @throws IOException
   */
  List<PdfFileContentResult> extractImages(MultipartFile file) throws IOException;
}