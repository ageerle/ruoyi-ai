package org.ruoyi.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.ruoyi.domain.PdfFileContentResult;
import org.ruoyi.service.DashscopeService;
import org.ruoyi.service.PdfImageExtractService;
import org.ruoyi.utils.ZipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF图片提取服务实现类
 */
@Service
@Slf4j
@Data
public class PdfImageExtractServiceImpl implements PdfImageExtractService {

  @Value("${pdf.extract.service.url}")
  private String serviceUrl;
  @Value("${pdf.extract.ai-api.url}")
  private String aiApiUrl;
  @Value("${pdf.extract.ai-api.key}")
  private String aiApiKey;

  @Autowired
  private DashscopeService dashscopeService;

  private final OkHttpClient client = new Builder()
      .connectTimeout(100, TimeUnit.SECONDS)
      .readTimeout(150, TimeUnit.SECONDS)
      .writeTimeout(150, TimeUnit.SECONDS)
      .callTimeout(300, TimeUnit.SECONDS)
      .build();

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  //  @Override
  public byte[] extractImages(MultipartFile pdfFile, String imageFormat, boolean allowDuplicates)
      throws IOException {
    // 构建multipart请求
    RequestBody requestBody = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("fileInput", pdfFile.getOriginalFilename(),
            RequestBody.create(MediaType.parse("application/pdf"), pdfFile.getBytes()))
        .addFormDataPart("format", imageFormat)
        .addFormDataPart("allowDuplicates", String.valueOf(allowDuplicates))
        .build();

    // 创建请求
    Request request = new Request.Builder()
        .url(serviceUrl + "/api/v1/misc/extract-images")
        .post(requestBody)
        .build();

    // 执行请求
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("请求失败: " + response.code());
      }
      return response.body().bytes();
    }
  }

  /**
   * 处理文件内容
   *
   * @param unzip Base64编码的图片数组
   * @return 文件内容结果列表
   * @throws IOException 如果API调用过程中发生错误
   */
//  @Override
  public List<PdfFileContentResult> dealFileContent(String[] unzip) throws IOException {
    List<PdfFileContentResult> results = new ArrayList<>();
    int i = 0;
    for (String base64Image : unzip) {
      // 构建请求JSON
      String requestJson = String.format("{"
          + "\"model\": \"gpt-4o\","
          + "\"stream\": false,"
          + "\"messages\": [{"
          + "\"role\": \"user\","
          + "\"content\": [{"
          + "\"type\": \"text\","
          + "\"text\": \"这张图片有什么\""
          + "}, {"
          + "\"type\": \"image_url\","
          + "\"image_url\": {"
          + "\"url\": \"%s\""
          + "}}"
          + "]}],"
          + "\"max_tokens\": 400"
          + "}", base64Image);

      // 创建请求
      Request request = new Request.Builder()
          .url(aiApiUrl)
          .addHeader("Authorization", "Bearer " + aiApiKey)
          .post(RequestBody.create(JSON, requestJson))
          .build();

      // 执行请求
      try {
        log.info("=============call=" + ++i);

        Response response = client.newCall(request).execute();
        log.info("=============response=" + response);
        if (!response.isSuccessful()) {
          throw new IOException("API请求失败: " + response.code() + response.toString());
        }

        String responseBody = response.body().string();
        log.info("=============responseBody=" + responseBody);
        // 使用文件名（这里使用base64的前10个字符作为标识）和API返回内容创建结果对象
        String filename = base64Image.substring(0, Math.min(base64Image.length(), 10));
        results.add(new PdfFileContentResult(filename, responseBody));
      } catch (Exception e) {
        log.error(e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return results;
  }

  /**
   * 利用百炼接口处理文件内容
   *
   * @param imageUrl 传入图片地址
   * @return 文件内容结果列表
   * @throws IOException 如果API调用过程中发生错误
   */
  @Override
  public List<PdfFileContentResult> dealFileContent4Dashscope(String imageUrl) throws IOException {
    String qvq = dashscopeService.qvq(imageUrl);
    // 构建结果列表
    List<PdfFileContentResult> results = new ArrayList<>();
    String filename = "image_" + System.currentTimeMillis();
    results.add(new PdfFileContentResult(filename, qvq));
    return results;
  }

  /**
   * 利用百炼接口处理文件内容
   *
   * 视觉推理（QVQ）  使用本地文件（输入Base64编码或本地路径）
   * @param localPath  图片文件的绝对路径
   * @return
   */
  @Override
  public List<PdfFileContentResult> dealFileContent4DashscopeBase64(String localPath) throws IOException {
    String qvq = dashscopeService.qvq4LocalPath(localPath);
    // 构建结果列表
    List<PdfFileContentResult> results = new ArrayList<>();
    String filename = "image_" + System.currentTimeMillis();
    results.add(new PdfFileContentResult(filename, qvq));
    return results;
  }



  //  @Override
  public List<PdfFileContentResult> extractImages(MultipartFile file) throws IOException {
    String format = "png";
    boolean allowDuplicates = true;
    // 获取ZIP数据
    byte[] zipData = this.extractImages(file, format, allowDuplicates);
    // 解压文件并识别图片内容并返回
    String[] unzip = ZipUtils.unzipForBase64(zipData);
    //解析图片内容
    return this.dealFileContent(unzip);
  }
}