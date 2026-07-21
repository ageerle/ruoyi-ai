package org.ruoyi.service.knowledge.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.knowledge.KnowledgeInfoBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeInfo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeInfoMapper;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.common.core.service.OssService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识库Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeInfoServiceImpl implements IKnowledgeInfoService {

    private final KnowledgeInfoMapper baseMapper;

    private final KnowledgeAttachMapper knowledgeAttachMapper;

    private final org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper knowledgeFragmentMapper;

    private final org.ruoyi.service.vector.VectorStoreService vectorStoreService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final OssService ossService;

    /**
     * 查询知识库
     *
     * @param id 主键
     * @return 知识库
     */
    @Override
    public KnowledgeInfoVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询知识库列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识库分页列表
     */
    @Override
    public TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        // 批量填充文档数
        fillDocumentCount(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的知识库列表
     *
     * @param bo 查询条件
     * @return 知识库列表
     */
    @Override
    public List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeInfo> buildQueryWrapper(KnowledgeInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeInfo::getId);
        lqw.eq(bo.getUserId() != null, KnowledgeInfo::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), KnowledgeInfo::getName, bo.getName());
        lqw.eq(bo.getShare() != null, KnowledgeInfo::getShare, bo.getShare());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getSeparator()), KnowledgeInfo::getSeparator, bo.getSeparator());
        lqw.eq(bo.getOverlapChar() != null, KnowledgeInfo::getOverlapChar, bo.getOverlapChar());
        lqw.eq(bo.getRetrieveLimit() != null, KnowledgeInfo::getRetrieveLimit, bo.getRetrieveLimit());
        lqw.eq(bo.getTextBlockSize() != null, KnowledgeInfo::getTextBlockSize, bo.getTextBlockSize());
        lqw.eq(StringUtils.isNotBlank(bo.getVectorModel()), KnowledgeInfo::getVectorModel, bo.getVectorModel());
        lqw.eq(StringUtils.isNotBlank(bo.getEmbeddingModel()), KnowledgeInfo::getEmbeddingModel, bo.getEmbeddingModel());
        return lqw;
    }

    /**
     * 批量填充知识库列表每一条记录的文档数（documentCount）
     */
    private void fillDocumentCount(List<KnowledgeInfoVo> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> ids = records.stream().map(KnowledgeInfoVo::getId).toList();
        Map<Long, Integer> counts = new java.util.HashMap<>();
        for (Map<String, Object> row : knowledgeAttachMapper.countByKnowledgeIds(ids)) {
            Number kid = (Number) (row.get("knowledgeId") != null ? row.get("knowledgeId") : row.get("knowledgeid"));
            Number count = (Number) (row.get("documentCount") != null ? row.get("documentCount") : row.get("documentcount"));
            if (kid != null && count != null) counts.put(kid.longValue(), count.intValue());
        }
        records.forEach(vo -> vo.setDocumentCount(counts.getOrDefault(vo.getId(), 0)));
    }

    /**
     * 新增知识库
     *
     * @param bo 知识库
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo add = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库
     *
     * @param bo 知识库
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo update = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(update);
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) knowledgeRetrievalService.invalidateKnowledge(String.valueOf(bo.getId()));
        return updated;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeInfo entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除知识库信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        for (Long kid : ids) {
            KnowledgeInfo info = baseMapper.selectById(kid);
            // 1. 删除向量库中该知识库的所有向量（按文档逐个清理，三种向量库行为一致）
            List<org.ruoyi.domain.entity.knowledge.KnowledgeAttach> attaches = knowledgeAttachMapper.selectList(
                Wrappers.lambdaQuery(org.ruoyi.domain.entity.knowledge.KnowledgeAttach.class)
                    .eq(org.ruoyi.domain.entity.knowledge.KnowledgeAttach::getKnowledgeId, kid));
            vectorStoreService.removeById(String.valueOf(kid), info == null ? null : info.getVectorModel());
            List<Long> ossIds = attaches.stream()
                    .map(org.ruoyi.domain.entity.knowledge.KnowledgeAttach::getOssId)
                    .filter(java.util.Objects::nonNull).toList();
            if (!ossIds.isEmpty()) {
                for (Long ossId : ossIds) {
                    ossService.deleteFile(ossId);
                }
            }
            // 2. 删除该知识库下的附件与片段记录
            knowledgeAttachMapper.delete(Wrappers.lambdaQuery(org.ruoyi.domain.entity.knowledge.KnowledgeAttach.class)
                .eq(org.ruoyi.domain.entity.knowledge.KnowledgeAttach::getKnowledgeId, kid));
            knowledgeFragmentMapper.delete(Wrappers.lambdaQuery(org.ruoyi.domain.entity.knowledge.KnowledgeFragment.class)
                .eq(org.ruoyi.domain.entity.knowledge.KnowledgeFragment::getKnowledgeId, kid));
            knowledgeRetrievalService.invalidateKnowledge(String.valueOf(kid));
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
