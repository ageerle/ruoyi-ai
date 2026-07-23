package org.ruoyi.trace;

/**
 * RAG trace 业务与节点类型常量。
 */
public final class RagTraceNodeTypes {

    private RagTraceNodeTypes() {
    }

    public static final String BUSINESS_TYPE_RAG_CHAT = "RAG_CHAT";
    public static final String TRACE_NAME_RAG_CHAT = "rag-chat";

    public static final String NODE_RETRIEVAL = "RETRIEVAL";
    public static final String NODE_RERANK = "RERANK";
    public static final String NODE_LLM_CALL = "LLM_CALL";
}
