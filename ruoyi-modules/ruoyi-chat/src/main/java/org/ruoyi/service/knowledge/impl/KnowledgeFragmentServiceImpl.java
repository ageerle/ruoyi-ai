package org.ruoyi.service.knowledge.impl;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.knowledge.KnowledgeFragmentBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.service.knowledge.IKnowledgeFragmentService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.service.knowledge.rerank.ScoringModelFactory;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Map;
import java.util.Collection;

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
    private final ScoringModelFactory scoringModelFactory;

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

        // 3. 执行物理检索
        List<KnowledgeRetrievalVo> allResults = vectorStoreService.search(queryVectorBo);

        // 初始化原始排名
        for (int i = 0; i < allResults.size(); i++) {
            allResults.get(i).setOriginalIndex(i);
        }

        // 4. 执行重排逻辑 (如果请求启用重排且配置了重排模型)
        if (Boolean.TRUE.equals(bo.getEnableRerank()) && StringUtils.isNotBlank(bo.getRerankModel())) {
            log.info("开始重排配置检索测试，传入模型名称: [{}]", bo.getRerankModel());
            ChatModelVo rerankModelConfig = chatModelService.selectModelByName(bo.getRerankModel());
            
            if (rerankModelConfig == null) {
                log.warn("未能找到重排模型配置: [{}]", bo.getRerankModel());
            } else {
                ScoringModel scoringModel = scoringModelFactory.createScoringModel(rerankModelConfig);
                if (scoringModel != null) {
                    log.info("执行重排精排，模型: {}, 供应商: {}", rerankModelConfig.getModelName(), rerankModelConfig.getProviderCode());

                    // 将 KnowledgeRetrievalVo 转换为 TextSegment 列表进行重排
                    List<TextSegment> segments = allResults.stream()
                        .map(res -> TextSegment.from(res.getContent()))
                        .collect(Collectors.toList());

                    Response<List<Double>> scoresResponse = scoringModel.scoreAll(segments, bo.getQuery());
                    List<Double> scores = scoresResponse.content();

                    // 更新分数并重新排序
                    for (int i = 0; i < allResults.size(); i++) {
                        KnowledgeRetrievalVo resultVo = allResults.get(i);
                        // 保存原始分数供前端展示对比
                        resultVo.setRawScore(resultVo.getScore());
                        if (i < scores.size()) {
                            resultVo.setScore(scores.get(i));
                        }
                    }

                    // 按重排后的分数从高到低排序
                    allResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
                }
            }
        }

        // 5. 根据阈值过滤
        double threshold = bo.getThreshold() != null ? bo.getThreshold() : 0.0;
        return allResults.stream()
                .filter(res -> res.getScore() >= threshold)
                .collect(Collectors.toList());
    }
}
