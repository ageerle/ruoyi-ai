package org.ruoyi.service.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.PdfFileContentResult;
import org.ruoyi.service.DashscopeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Description: 阿里百炼API
 * @Date: 2025/6/4 下午2:28
 */
@Service
@Slf4j
public class DashscopeServiceImpl implements DashscopeService {

  private boolean isFirstPrint;

  @Value("${dashscope.model}")
  private String serviceModel;
  @Value("${dashscope.key}")
  private String serviceKey;

  /**
   * 视觉推理（QVQ）
   * @param imageUrl  图片可访问地址
   * @return
   */
  @Override
  public String qvq(String imageUrl) throws IOException {
    try {
      // 构建多模态消息
      MultiModalMessage userMessage = MultiModalMessage.builder()
          .role(Role.USER.getValue())
          .content(Arrays.asList(
              Collections.singletonMap("text", "这张图片有什么"),
              Collections.singletonMap("image", imageUrl)
          ))
          .build();

      // 构建请求参数
      MultiModalConversationParam param = MultiModalConversationParam.builder()
          .apiKey(serviceKey) // 使用配置文件中的API Key
          .model(serviceModel)
          .message(userMessage)
          .build();

      MultiModalConversation conv = new MultiModalConversation();

      // 调用API
      Flowable<MultiModalConversationResult> result = conv.streamCall(
          param);

      StringBuilder reasoningContent = new StringBuilder();
      StringBuilder finalContent = new StringBuilder();
      isFirstPrint = true;

      result.blockingForEach(message -> handleGenerationResult(message, reasoningContent, finalContent));

      return finalContent.toString().replaceAll("[\n\r\s]", "");
    } catch (Exception e) {
      log.error("调用百炼API失败: {}", e.getMessage(), e);
      throw new IOException("百炼API调用失败: " + e.getMessage(), e);
    }
  }
  /**
   * 视觉推理（QVQ）  使用本地文件（输入Base64编码或本地路径）
   * @param localPath  图片文件的绝对路径
   * @return
   */
  @Override
  public String qvq4LocalPath(String localPath) throws IOException {
    try {
      // 构建多模态消息
     String filePath = "file://"+ localPath;
     log.info("filePath: {}", filePath);
      MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
          .content(Arrays.asList(new HashMap<String, Object>(){{put("image", filePath);}},
              new HashMap<String, Object>(){{put("text", "这张图片有什么");}})).build();

      // 构建请求参数
      MultiModalConversationParam param = MultiModalConversationParam.builder()
          .apiKey(serviceKey) // 使用配置文件中的API Key
          .model(serviceModel)
          .message(userMessage)
          .build();
      MultiModalConversation conv = new MultiModalConversation();

      // 调用API
      Flowable<MultiModalConversationResult> result = conv.streamCall(
          param);

      StringBuilder reasoningContent = new StringBuilder();
      StringBuilder finalContent = new StringBuilder();
      isFirstPrint = true;

      result.blockingForEach(message -> handleGenerationResult(message, reasoningContent, finalContent));

      return finalContent.toString().replaceAll("[\n\r\s]", "");
    } catch (Exception e) {
      log.error("调用百炼API失败: {}", e.getMessage(), e);
      throw new IOException("百炼API调用失败: " + e.getMessage(), e);
    }
  }


  private void handleGenerationResult(MultiModalConversationResult message, StringBuilder reasoningContent, StringBuilder finalContent) {
    String re = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
    String reasoning = Objects.isNull(re) ? "" : re;

    List<Map<String, Object>> content = message.getOutput().getChoices().get(0).getMessage()
        .getContent();
    if (!reasoning.isEmpty()) {
      reasoningContent.append(reasoning);
    }

    if (Objects.nonNull(content) && !content.isEmpty()) {
      Object text = content.get(0).get("text");
      finalContent.append(text);
    }

    // 检查是否是最后一个响应
    if (message.getOutput().getChoices().get(0).getFinishReason() != null) {
      // 输出思考过程
      if (reasoningContent.length() > 0) {
        System.out.println("====================思考过程====================");
        System.out.println(reasoningContent.toString());
      }
      // 输出完整回复
      if (finalContent.length() > 0) {
        System.out.println("====================完整回复====================");
        System.out.println(finalContent.toString());
      }
    }
  }
}
