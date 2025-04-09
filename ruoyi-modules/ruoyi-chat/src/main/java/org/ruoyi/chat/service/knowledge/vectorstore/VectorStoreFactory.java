package org.ruoyi.chat.service.knowledge.vectorstore;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.mapper.KnowledgeInfoMapper;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VectorStoreFactory {

    private final WeaviateVectorStore weaviateVectorStore;

    private final MilvusVectorStore milvusVectorStore;

    @Resource
    private KnowledgeInfoMapper knowledgeInfoMapper;

    public VectorStoreFactory(WeaviateVectorStore weaviateVectorStore, MilvusVectorStore milvusVectorStore) {
        this.weaviateVectorStore = weaviateVectorStore;
        this.milvusVectorStore = milvusVectorStore;
    }

    public VectorStoreService getVectorStore(String kid){
        String vectorModel = "weaviate";
        if (StrUtil.isNotEmpty(kid)) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoMapper.selectVoById(Long.valueOf(kid));
            if (knowledgeInfoVo != null && StrUtil.isNotEmpty(knowledgeInfoVo.getVector())) {
                vectorModel = knowledgeInfoVo.getVector();
            }
        }
        if ("weaviate".equals(vectorModel)){
            return weaviateVectorStore;
        }else if ("milvus".equals(vectorModel)){
            return milvusVectorStore;
        }
        return null;
    }
}
