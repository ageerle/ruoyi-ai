package org.ruoyi.knowledge.chain.vectorizer;

public enum VectorizationType {
    OPENAI,    // OpenAI 向量化
    LOCAL;     // 本地模型向量化

    public static VectorizationType fromString(String type) {
        for (VectorizationType v : values()) {
            if (v.name().equalsIgnoreCase(type)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown VectorizationType: " + type);
    }
}
