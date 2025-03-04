package org.ruoyi.knowledge.chain.vectorstore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VectorStoreFactory {

    @Value("${chain.vector.store.type}")
    private String type;

    private final WeaviateVectorStore weaviateVectorStore;

    private final MilvusVectorStore milvusVectorStore;

    public VectorStoreFactory(WeaviateVectorStore weaviateVectorStore, MilvusVectorStore milvusVectorStore) {
        this.weaviateVectorStore = weaviateVectorStore;
        this.milvusVectorStore = milvusVectorStore;
    }

    public VectorStore getVectorStore(){
        if ("weaviate".equals(type)){
            return weaviateVectorStore;
        }else if ("milvus".equals(type)){
            return milvusVectorStore;
        }
        return null;
    }
}
