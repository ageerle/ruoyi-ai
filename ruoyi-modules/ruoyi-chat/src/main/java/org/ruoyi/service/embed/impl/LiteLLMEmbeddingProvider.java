package org.ruoyi.service.embed.impl;

import org.springframework.stereotype.Component;

/**
 * LiteLLM embedding model (OpenAI-compatible).
 * <p>
 * LiteLLM proxy speaks the OpenAI embeddings protocol, so this reuses the OpenAI
 * embedding builder. Point apiHost at the LiteLLM proxy to generate embeddings
 * through any LiteLLM-supported provider (OpenAI, Cohere, Voyage, Bedrock ...).
 *
 * @author ageerle@163.com
 */
@Component("litellm")
@org.springframework.context.annotation.Scope("prototype")
public class LiteLLMEmbeddingProvider extends OpenAiEmbeddingProvider {

}
