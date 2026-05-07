package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 千问模型测试类
 * 测试不同模型名称是否可用
 */
class QianWenChatServiceTest {

    // 请替换为你的 API Key
    private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");

    // 测试不同的模型名称
    private static final String[] MODEL_NAMES = {
        "qwen-turbo",
        "qwen-plus",
        "qwen-max",
        "qwen3.6-flash",  // 用户配置的模型名称
        "qwen3.6-plus"    // 用户配置的模型名称
    };

    @Test
    void testQwenModels() throws InterruptedException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("跳过测试: 未设置环境变量 DASHSCOPE_API_KEY");
            System.out.println("请设置环境变量: export DASHSCOPE_API_KEY=your-api-key");
            return;
        }

        System.out.println("========================================");
        System.out.println("千问模型名称测试");
        System.out.println("========================================\n");

        for (String modelName : MODEL_NAMES) {
            testModel(modelName);
            System.out.println();
        }
    }

    private void testModel(String modelName) throws InterruptedException {
        System.out.println(">>> 测试模型: " + modelName);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        try {
            StreamingChatModel model = QwenStreamingChatModel.builder()
                .apiKey(API_KEY)
                .modelName(modelName)
                .build();

            model.chat("你好，请说'测试成功'", new StreamingChatResponseHandler() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        buffer.append(partialResponse);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    result.set(buffer.toString());
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                    latch.countDown();
                }
            });

            // 等待响应，最多 30 秒
            boolean completed = latch.await(30, TimeUnit.SECONDS);

            if (!completed) {
                System.out.println("    结果: 超时（30秒无响应）");
                return;
            }

            if (error.get() != null) {
                System.out.println("    结果: 失败");
                System.out.println("    错误: " + error.get().getMessage());
            } else {
                System.out.println("    结果: 成功 ✓");
                String response = result.get();
                if (response != null && response.length() > 0) {
                    System.out.println("    响应: " + response.substring(0, Math.min(100, response.length())) + "...");
                }
            }

        } catch (Exception e) {
            System.out.println("    结果: 异常");
            System.out.println("    错误: " + e.getMessage());
        }
    }
}
