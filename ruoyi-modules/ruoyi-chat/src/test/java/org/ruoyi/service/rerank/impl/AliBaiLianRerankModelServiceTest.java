package org.ruoyi.service.rerank.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阿里百炼重排序模型测试类
 * 运行前请设置环境变量 DASHSCOPE_API_KEY 或直接修改 apiKey
 */
class AliBaiLianRerankModelServiceTest {

    private AliBaiLianRerankModelService service;

    // 请替换为你的 API Key
    private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");
    private static final String API_HOST = "https://dashscope.aliyuncs.com";
    private static final String MODEL_NAME = "qwen3-rerank";

    @BeforeEach
    void setUp() {
        service = new AliBaiLianRerankModelService();
    }

    @Test
    void testConfigure() {
        ChatModelVo config = createConfig();
        service.configure(config);
        assertNotNull(service);
    }

    @Test
    void testRerank() {
        // 跳过测试如果没有配置 API Key
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("跳过测试: 未设置环境变量 DASHSCOPE_API_KEY");
            return;
        }

        ChatModelVo config = createConfig();
        service.configure(config);

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

        RerankResult result = service.rerank(request);

        System.out.println("=== 重排序结果 ===");
        System.out.println("总文档数: " + result.getTotalDocuments());
        System.out.println("耗时: " + result.getDurationMs() + "ms");

        result.getDocuments().forEach(doc -> {
            System.out.println("索引: " + doc.getIndex() +
                    ", 相关性分数: " + doc.getRelevanceScore() +
                    ", 文档: " + doc.getDocument());
        });

        assertNotNull(result);
        assertNotNull(result.getDocuments());
        assertFalse(result.getDocuments().isEmpty());
        assertEquals(2, result.getDocuments().size());
    }

    @Test
    void testRerankWithFullDocuments() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("跳过测试: 未设置环境变量 DASHSCOPE_API_KEY");
            return;
        }

        ChatModelVo config = createConfig();
        service.configure(config);

        List<String> documents = Arrays.asList(
                "Java是一种广泛使用的编程语言",
                "Python是人工智能领域最流行的语言",
                "Go语言由Google开发，适合并发编程"
        );

        RerankRequest request = RerankRequest.builder()
                .query("哪种语言适合AI开发")
                .documents(documents)
                .build();

        RerankResult result = service.rerank(request);

        System.out.println("=== 重排序结果2 ===");
        result.getDocuments().forEach(doc -> {
            System.out.println("索引: " + doc.getIndex() +
                    ", 分数: " + doc.getRelevanceScore() +
                    ", 文档: " + doc.getDocument());
        });

        assertNotNull(result);
        assertEquals(3, result.getDocuments().size());

        // Python相关文档应该排在前面
        assertEquals(1, result.getDocuments().get(0).getIndex());
        assertTrue(result.getDocuments().get(0).getRelevanceScore() > 0.5);
    }

    private ChatModelVo createConfig() {
        ChatModelVo config = new ChatModelVo();
        config.setApiHost(API_HOST);
        config.setApiKey(API_KEY != null ? API_KEY : "test-api-key");
        config.setModelName(MODEL_NAME);
        return config;
    }
}
