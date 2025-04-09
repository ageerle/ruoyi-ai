package org.ruoyi.chat.service.knowledge.vectorizer;

import com.google.gson.Gson;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.embeddings.OllamaEmbeddingsRequestModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.VectorizationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BgeLargeVectorization implements VectorizationService {

    String host = "http://localhost:11434/";

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        OllamaAPI ollamaAPI = new OllamaAPI(host);
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid));
        List<Double> doubleVector;
        try {
            doubleVector = ollamaAPI.generateEmbeddings(new OllamaEmbeddingsRequestModel(knowledgeInfoVo.getVectorModel(), new Gson().toJson(chunkList)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<List<Double>> vectorList = new ArrayList<>();
        vectorList.add(doubleVector);
        return vectorList;
    }

    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        List<String> chunkList = new ArrayList<>();
        chunkList.add(chunk);
        List<List<Double>> vectorList = batchVectorization(chunkList, kid);
        return vectorList.get(0);
    }

    public static void main(String[] args) {
        OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434/");
        List<String> chunkList = Arrays.asList("天很蓝", "海很深");
        List<Double> doubleVector;
        try {
            doubleVector = ollamaAPI.generateEmbeddings(new OllamaEmbeddingsRequestModel("quentinz/bge-large-zh-v1.5", new Gson().toJson(chunkList)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("=== " + doubleVector + " 1===");
    }
}
