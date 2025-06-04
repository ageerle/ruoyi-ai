package org.ruoyi.service;

import java.io.IOException;

/**
 * @Description: 阿里百炼api
 * @Date: 2025/6/4 下午2:24
 */
public interface DashscopeService {

  /**
   * 视觉推理（QVQ）
   * @param imageUrl 图片可访问的地址
   * @return
   */
  String qvq(String imageUrl) throws IOException;
  /**
   * 视觉推理（QVQ）  使用本地文件（输入Base64编码或本地路径）
   * @param localPath  图片文件的绝对路径
   * @return
   */
  String qvq4LocalPath(String localPath) throws IOException;
}
