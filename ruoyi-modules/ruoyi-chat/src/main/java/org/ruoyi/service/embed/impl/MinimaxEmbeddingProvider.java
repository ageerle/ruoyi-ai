package org.ruoyi.service.embed.impl;

import org.springframework.stereotype.Component;

/**
 * MiniMax嵌入模型（兼容OpenAI接口）
 * <p>
 * 支持embo-01模型，1536维度向量。
 * API地址：https://api.minimax.io/v1
 *
 * @author octopus
 * @date 2026/3/21
 */
@Component("minimax")
public class MinimaxEmbeddingProvider extends OpenAiEmbeddingProvider {

}
