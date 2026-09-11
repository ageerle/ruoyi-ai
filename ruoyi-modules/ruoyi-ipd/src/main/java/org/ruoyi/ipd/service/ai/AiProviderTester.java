package org.ruoyi.ipd.service.ai;

import java.util.Set;

/**
 * P4-2.1 AI 多协议 Tester 抽象（BR-AI-PROV-01/02）。
 * <p>
 * 各 Provider 走自己的协议（鉴权头、路径、body 形态）；ProviderRegistry 按 provider / aliases 派发。
 * 实现要点：errorMessage 不得含 apiKey/请求头；测试结果仅返回结构化字段，不抛出业务异常。
 */
public interface AiProviderTester {

    /** 主 provider 名（用于审计/默认匹配） */
    String provider();

    /** 该 tester 兼容的 provider 集合（大小写不敏感）。 */
    Set<String> aliases();

    /** 探测连接是否可用——失败消息不得含 apiKey/密文片段（BR-AI-PROV-02）。 */
    AiTestResult test(AiTestConfig cfg);
}
