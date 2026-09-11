package org.ruoyi.ipd.service.ai;

/**
 * P4-2.1 AI 协议测试入参（BR-AI-PROV-04：timeoutMs 默认 5s）。
 * <p>
 * apiKey 在 Tester 内部仅作内存调用（HTTP 鉴权头），不得出现在日志或 {@code AiTestResult.errorMessage}。
 *
 * @param provider   供应商标识（openai/deepseek/qwen/moonshot/MiniMax/zhipu/glm/baidu/qianfan/ollama/default）
 * @param baseUrl    用户配置的接入端点
 * @param apiKey     明文密钥（仅 Tester 内部消化用，绝不外传）
 * @param modelName  模型名（用于 OpenAI 兼容 POST body）
 * @param timeoutMs  超时（连接+读），默认 5000
 */
public record AiTestConfig(String provider, String baseUrl, String apiKey, String modelName, int timeoutMs) {

    public AiTestConfig {
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
    }

    public AiTestConfig(String provider, String baseUrl, String apiKey, String modelName) {
        this(provider, baseUrl, apiKey, modelName, 5000);
    }
}
