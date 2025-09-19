
  /**
   * 处理文件内容
   *
   * @param unzip Base64编码的图片数组
   * @return 文件内容结果列表
   * @throws IOException 如果API调用过程中发生错误
   */
  List<PdfFileContentResult> dealFileContent(String[] unzip) throws IOException;
  /**
   *利用百炼接口处理文件内容
   *
   * @param imageUrl 传入图片地址
   * @return 文件内容结果列表
   * @throws IOException 如果API调用过程中发生错误
   */
  List<PdfFileContentResult> dealFileContent4Dashscope(String imageUrl) throws IOException;

  /**
   * 利用百炼接口处理文件内容
   *
   * 视觉推理（QVQ）  使用本地文件（输入Base64编码或本地路径）
   * @param localPath  图片文件的绝对路径
   * @return
   */
  List<PdfFileContentResult> dealFileContent4DashscopeBase64(String localPath)throws IOException;
  /**
   * 提取PDF中的图片并调用gpt-4o-mini,识别图片内容并返回
   * @param file
   * @return
   * @throws IOException
   */
  List<PdfFileContentResult> extractImages(MultipartFile file) throws IOException;
}
