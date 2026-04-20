package org.ruoyi.service.rerank.impl;

import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;

import java.util.Arrays;
import java.util.List;

/**
 * 阿里百炼重排序模型测试 - Main方法直接运行
 * 运行前请设置 API_KEY
 */
public class AliBaiLianRerankTestMain {

    // 请替换为你的 API Key
    private static final String API_KEY = "sk-your-api-key-here";
    private static final String API_HOST = "https://dashscope.aliyuncs.com";
    private static final String MODEL_NAME = "qwen3-rerank";

    public static void main(String[] args) {
        AliBaiLianRerankModelService service = new AliBaiLianRerankModelService();

        // 配置
        ChatModelVo config = new ChatModelVo();
        config.setApiHost(API_HOST);
        config.setApiKey(API_KEY);
        config.setModelName(MODEL_NAME);
        service.configure(config);

        // 测试数据
        List<String> documents = Arrays.asList(
                "文本排序模型广泛用于搜索引擎和推荐系统中，它们根据文本相关性对候选文本进行排序",
                "量子计算是计算科学的一个前沿领域",
                "预训练语言模型的发展给文本排序模型带来了新的进展"
        );

        RerankRequest request = RerankRequest.builder()
                .query("什么是文本排序模型")
                .documents(documents)
                .topN(2)
                .returnDocuments(true)
                .build();

        System.out.println("=== 开始测试阿里百炼重排序 ===");
        System.out.println("API Host: " + API_HOST);
        System.out.println("Model: " + MODEL_NAME);
        System.out.println("Query: 什么是文本排序模型");
        System.out.println();

        try {
            RerankResult result = service.rerank(request);

            System.out.println("=== 重排序结果 ===");
            System.out.println("总文档数: " + result.getTotalDocuments());
            System.out.println("耗时: " + result.getDurationMs() + "ms");
            System.out.println();

            result.getDocuments().forEach(doc -> {
                System.out.println("索引: " + doc.getIndex());
                System.out.println("相关性分数: " + doc.getRelevanceScore());
                System.out.println("文档: " + doc.getDocument());
                System.out.println("---");
            });

            System.out.println("=== 测试成功 ===");

        } catch (Exception e) {
            System.err.println("=== 测试失败 ===");
            e.printStackTrace();
        }
    }
}
