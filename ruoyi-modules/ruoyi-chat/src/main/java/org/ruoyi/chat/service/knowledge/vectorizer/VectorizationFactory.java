package org.ruoyi.chat.service.knowledge.vectorizer;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.VectorizationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 文本向量化
 * @author huangkh
 */
@Component
@Slf4j
public class VectorizationFactory {

    private final OpenAiVectorization openAiVectorization;

    private final BgeLargeVectorization bgeLargeVectorization;

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    public VectorizationFactory(OpenAiVectorization openAiVectorization, BgeLargeVectorization bgeLargeVectorization) {
        this.openAiVectorization = openAiVectorization;
        this.bgeLargeVectorization = bgeLargeVectorization;
    }

    public VectorizationService getEmbedding(String kid){
        String vectorModel = "text-embedding-3-small";
        if (StrUtil.isNotEmpty(kid)) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid));
            if (knowledgeInfoVo != null && StrUtil.isNotEmpty(knowledgeInfoVo.getVectorModel())) {
                vectorModel = knowledgeInfoVo.getVectorModel();
            }
        }
        return switch (vectorModel) {
            case "quentinz/bge-large-zh-v1.5" -> bgeLargeVectorization;
            default -> openAiVectorization;
        };
    }
}
