package org.ruoyi.knowledge.chain.vectorstore;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.knowledge.domain.KnowledgeInfo;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.mapper.KnowledgeInfoMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VectorStoreFactory {

    private final WeaviateVectorStore weaviateVectorStore;

    private final MilvusVectorStore milvusVectorStore;

    private final  KnowledgeInfoMapper knowledgeInfoMapper;

    public VectorStore getVectorStore(String kid){
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoMapper.selectVoOne(
                new LambdaQueryWrapper<KnowledgeInfo>().eq(KnowledgeInfo::getKid,kid)
        );
        String vectorModel = knowledgeInfoVo.getVector();
        if ("weaviate".equals(vectorModel)){
            return weaviateVectorStore;
        }else if ("milvus".equals(vectorModel)){
            return milvusVectorStore;
        }
        return null;
    }
}
