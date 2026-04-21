package org.ruoyi.service.knowledge.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.knowledge.KnowledgeFragmentBo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.RerankModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.knowledge.IKnowledgeFragmentService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.rerank.RerankModelService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


/**
 * 知识片段Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeFragmentServiceImpl implements IKnowledgeFragmentService {

    private final KnowledgeFragmentMapper baseMapper;
    private final IKnowledgeInfoService knowledgeInfoService;
    private final IChatModelService chatModelService;
    private final VectorStoreService vectorStoreService;
    private final RerankModelFactory rerankModelFactory;

    /**
     * 查询知识片段
     *
     * @param id 主键
     * @return 知识片段
     */
    @Override
    public KnowledgeFragmentVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询知识片段列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识片段分页列表
     */
    @Override
    public TableDataInfo<KnowledgeFragmentVo> queryPageList(KnowledgeFragmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeFragment> lqw = buildQueryWrapper(bo);
        Page<KnowledgeFragmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的知识片段列表
     *
     * @param bo 查询条件
     * @return 知识片段列表
     */
    @Override
    public List<KnowledgeFragmentVo> queryList(KnowledgeFragmentBo bo) {
        LambdaQueryWrapper<KnowledgeFragment> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeFragment> buildQueryWrapper(KnowledgeFragmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeFragment> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeFragment::getId);
        lqw.eq(bo.getDocId() != null, KnowledgeFragment::getDocId, bo.getDocId());
        lqw.eq(bo.getIdx() != null, KnowledgeFragment::getIdx, bo.getIdx());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), KnowledgeFragment::getContent, bo.getContent());
        return lqw;
    }

    /**
     * 新增知识片段
     *
     * @param bo 知识片段
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(KnowledgeFragmentBo bo) {
        KnowledgeFragment add = MapstructUtils.convert(bo, KnowledgeFragment.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识片段
     *
     * @param bo 知识片段
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(KnowledgeFragmentBo bo) {
        KnowledgeFragment update = MapstructUtils.convert(bo, KnowledgeFragment.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeFragment entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除知识片段信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 检索测试核心实现
     */
    @Override
    public List<KnowledgeRetrievalVo> retrieval(KnowledgeFragmentBo bo) {
        if (bo.getKnowledgeId() == null || StringUtils.isBlank(bo.getQuery())) {
            return new ArrayList<>();
        }

        // 1. 获取知识库及模型配置
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(bo.getKnowledgeId());
        if (knowledgeInfoVo == null) {
            return new ArrayList<>();
        }

        ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
        if (chatModel == null) {
            log.warn("未找到对应的向量模型配置: {}", knowledgeInfoVo.getEmbeddingModel());
            return new ArrayList<>();
        }

        // 2. 构造向量检索参数
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(bo.getQuery());
        queryVectorBo.setKid(String.valueOf(bo.getKnowledgeId()));
        queryVectorBo.setMaxResults(bo.getTopK() != null ? bo.getTopK() : knowledgeInfoVo.getRetrieveLimit());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setApiKey(chatModel.getApiKey());
        queryVectorBo.setBaseUrl(chatModel.getApiHost());

        // 3. 执行搜索 (向量搜索 + 关键词搜索)
        List<KnowledgeRetrievalVo> allResults;
        
        boolean hybridEnabled = Boolean.TRUE.equals(bo.getEnableHybrid()) || 
                             Integer.valueOf(1).equals(knowledgeInfoVo.getEnableHybrid());
        
        if (hybridEnabled) {
            log.info("执行混合检索: kid={}, query={}", bo.getKnowledgeId(), bo.getQuery());
            try {
                // 并行执行向量搜索
                CompletableFuture<List<KnowledgeRetrievalVo>> vectorFuture = CompletableFuture.supplyAsync(() -> 
                    vectorStoreService.search(queryVectorBo));
                
                // 执行关键词搜索 (MySQL)
                int limit = bo.getTopK() != null ? bo.getTopK() : 50;
                List<KnowledgeFragmentVo> keywordFragments = baseMapper.searchByKeyword(bo.getKnowledgeId(), bo.getQuery(), limit);
                List<KnowledgeRetrievalVo> keywordResults = keywordFragments.stream().map(f -> {
                    KnowledgeRetrievalVo vo = new KnowledgeRetrievalVo();
                    vo.setId(f.getId().toString());
                    vo.setContent(f.getContent());
                    vo.setDocId(f.getDocId());
                    vo.setIdx(f.getIdx());
                    vo.setKnowledgeId(f.getKnowledgeId());
                    vo.setScore(10.0); // 初始分，后续由 RRF 重新打分
                    return vo;
                }).collect(Collectors.toList());
                
                List<KnowledgeRetrievalVo> vectorResults = vectorFuture.get();
                log.info("抽取混合结果成功: Vector命中={}条, Keyword命中={}条", vectorResults.size(), keywordResults.size());

                double alpha = bo.getHybridAlpha() != null ? bo.getHybridAlpha() : 
                              (knowledgeInfoVo.getHybridAlpha() != null ? knowledgeInfoVo.getHybridAlpha() : 0.5);
                
                allResults = calculateRRF(vectorResults, keywordResults, alpha);
            } catch (Exception e) {
                log.error("混合检索执行或合并失败，已自动降级回退到纯向量检索", e);
                allResults = vectorStoreService.search(queryVectorBo);
            }
        } else {
            allResults = vectorStoreService.search(queryVectorBo);
        }

        // 初始化原始排名
        for (int i = 0; i < allResults.size(); i++) {
            allResults.get(i).setOriginalIndex(i);
        }

        // 4. 执行重排逻辑 (如果请求启用重排且配置了重排模型)
        if (Boolean.TRUE.equals(bo.getEnableRerank()) && StringUtils.isNotBlank(bo.getRerankModel())) {
            log.info("开始重排精排，模型: [{}]", bo.getRerankModel());
            try {
                RerankModelService rerankModel = rerankModelFactory.createModel(bo.getRerankModel());

                List<String> contents = allResults.stream()
                        .map(KnowledgeRetrievalVo::getContent)
                        .collect(Collectors.toList());

                RerankRequest rerankRequest = RerankRequest.builder()
                        .query(bo.getQuery())
                        .documents(contents)
                        .topN(contents.size())
                        .returnDocuments(false)
                        .build();

                RerankResult rerankResult = rerankModel.rerank(rerankRequest);

                // 将重排分数写回，并记录原始分数供前端对比
                for (RerankResult.RerankDocument doc : rerankResult.getDocuments()) {
                    if (doc.getIndex() != null && doc.getIndex() < allResults.size()) {
                        KnowledgeRetrievalVo resultVo = allResults.get(doc.getIndex());
                        resultVo.setRawScore(resultVo.getScore());
                        resultVo.setScore(doc.getRelevanceScore());
                    }
                }

                // 按重排后的分数从高到低排序
                allResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
                log.info("重排精排完成，结果数: {}", allResults.size());

            } catch (Exception e) {
                log.error("重排精排执行失败，已跳过重排步骤: {}", e.getMessage(), e);
            }
        }

        // 5. 根据阈值过滤
        double threshold = bo.getThreshold() != null ? bo.getThreshold() : 0.0;
        return allResults.stream()
                .filter(res -> res.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合算法
     * 公式: Score = (1-alpha) * (1 / (k + rank_vector)) + alpha * (1 / (k + rank_keyword))
     */
    private List<KnowledgeRetrievalVo> calculateRRF(List<KnowledgeRetrievalVo> vectorList, List<KnowledgeRetrievalVo> keywordList, double alpha) {
        Map<String, KnowledgeRetrievalVo> allMap = new HashMap<>();
        Map<String, Double> vectorScores = new HashMap<>();
        Map<String, Double> keywordScores = new HashMap<>();

        int k = 60; // 常用 RRF 常数

        for (int i = 0; i < vectorList.size(); i++) {
            KnowledgeRetrievalVo vo = vectorList.get(i);
            allMap.put(vo.getId(), vo);
            vectorScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        for (int i = 0; i < keywordList.size(); i++) {
            KnowledgeRetrievalVo vo = keywordList.get(i);
            if (!allMap.containsKey(vo.getId())) {
                allMap.put(vo.getId(), vo);
            }
            keywordScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        // 重新计算得分
        List<KnowledgeRetrievalVo> fusedResults = new ArrayList<>();
        for (Map.Entry<String, KnowledgeRetrievalVo> entry : allMap.entrySet()) {
            String id = entry.getKey();
            double vScore = vectorScores.getOrDefault(id, 0.0);
            double kScore = keywordScores.getOrDefault(id, 0.0);
            
            // 混合分值
            double finalScore = (1 - alpha) * vScore + alpha * kScore;
            
            // 分值归一化/缩放：将 RRF 分值放大到 0-1 范围
            // 理论单路最大得分为 1/61 ≈ 0.016，乘以 60 使其处于相似度常用区间
            KnowledgeRetrievalVo vo = entry.getValue();
            vo.setScore(finalScore * 60.0); 
            fusedResults.add(vo);
        }

        // 按融合分数从高到低排序
        fusedResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        return fusedResults;
    }
}
