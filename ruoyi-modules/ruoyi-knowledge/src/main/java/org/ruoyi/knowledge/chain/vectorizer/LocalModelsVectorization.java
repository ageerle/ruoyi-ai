package org.ruoyi.knowledge.chain.vectorizer;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.config.ChatConfig;
import org.ruoyi.common.chat.localModels.LocalModelsofitClient;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocalModelsVectorization   {
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    @Resource
    private LocalModelsofitClient localModelsofitClient;

    @Getter
    private OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;

    /**
     * 批量向量化
     *
     * @param chunkList 文本块列表
     * @param kid 知识 ID
     * @return 向量化结果
     */

    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        logVectorizationRequest(kid, chunkList);  // 在向量化开始前记录日志
        openAiStreamClient = chatConfig.getOpenAiStreamClient(); // 获取 OpenAi 客户端
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid)); // 查询知识信息
        // 调用 localModelsofitClient 获取 Top K 嵌入向量
        try {
            return localModelsofitClient.getTopKEmbeddings(
                    chunkList,
                    knowledgeInfoVo.getVectorModel(),
                    knowledgeInfoVo.getKnowledgeSeparator(),
                    knowledgeInfoVo.getRetrieveLimit(),
                    knowledgeInfoVo.getTextBlockSize(),
                    knowledgeInfoVo.getOverlapChar()
            );
        } catch (Exception e) {
            log.error("Failed to perform batch vectorization for knowledgeId: {}", kid, e);
            throw new RuntimeException("Batch vectorization failed", e);
        }
    }

    /**
     * 单一文本块向量化
     *
     * @param chunk 单一文本块
     * @param kid 知识 ID
     * @return 向量化结果
     */

    public List<Double> singleVectorization(String chunk, String kid) {
        List<String> chunkList = new ArrayList<>();
        chunkList.add(chunk);

        // 调用批量向量化方法
        List<List<Double>> vectorList = batchVectorization(chunkList, kid);

        if (vectorList.isEmpty()) {
            log.warn("Vectorization returned empty list for chunk: {}", chunk);
            return new ArrayList<>();
        }

        return vectorList.get(0); // 返回第一个向量
    }

    /**
     * 提供更简洁的日志记录方法
     *
     * @param kid 知识 ID
     * @param chunkList 文本块列表
     */
    private void logVectorizationRequest(String kid, List<String> chunkList) {
        log.info("Starting vectorization for Knowledge ID: {} with {} chunks.", kid, chunkList.size());
    }
}
