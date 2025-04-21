package org.ruoyi.chat.service.knowledge;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.embeddings.OllamaEmbeddingsRequestModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.VectorizationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ageer
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BgeLargeVectorizationImpl implements VectorizationService {

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    @Lazy
    @Resource
    private final IChatModelService chatModelService;

    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {

        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid));

        ChatModelVo chatModelVo = chatModelService.selectModelByName(knowledgeInfoVo.getVectorModel());

        OllamaAPI api = new OllamaAPI(chatModelVo.getApiHost());

        List<Double> doubleVector;
        List<List<Double>> vectorList = new ArrayList<>();
        try {
            for (String chunk : chunkList) {
                doubleVector = api.generateEmbeddings(new OllamaEmbeddingsRequestModel(knowledgeInfoVo.getVectorModel(), chunk));
                vectorList.add(doubleVector);
            }
        } catch (Exception e) {
            throw new ServiceException("文本向量化异常："+e.getMessage());
        }
        return vectorList;
    }

    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        List<String> chunkList = new ArrayList<>();
        chunkList.add(chunk);
        List<List<Double>> vectorList = batchVectorization(chunkList, kid);
        return vectorList.get(0);
    }

}
