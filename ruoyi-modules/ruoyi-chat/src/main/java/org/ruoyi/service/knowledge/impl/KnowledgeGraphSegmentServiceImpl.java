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
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphSegmentBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeGraphSegment;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphSegmentVo;
import org.ruoyi.mapper.knowledge.KnowledgeGraphSegmentMapper;
import org.ruoyi.service.knowledge.IKnowledgeGraphSegmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识图谱片段Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeGraphSegmentServiceImpl implements IKnowledgeGraphSegmentService {

    private final KnowledgeGraphSegmentMapper baseMapper;

    /**
     * 查询知识图谱片段
     *
     * @param id 主键
     * @return 知识图谱片段
     */
    @Override
    public KnowledgeGraphSegmentVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询知识图谱片段列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识图谱片段分页列表
     */
    @Override
    public TableDataInfo<KnowledgeGraphSegmentVo> queryPageList(KnowledgeGraphSegmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeGraphSegment> lqw = buildQueryWrapper(bo);
        Page<KnowledgeGraphSegmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的知识图谱片段列表
     *
     * @param bo 查询条件
     * @return 知识图谱片段列表
     */
    @Override
    public List<KnowledgeGraphSegmentVo> queryList(KnowledgeGraphSegmentBo bo) {
        LambdaQueryWrapper<KnowledgeGraphSegment> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeGraphSegment> buildQueryWrapper(KnowledgeGraphSegmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeGraphSegment> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeGraphSegment::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getUuid()), KnowledgeGraphSegment::getUuid, bo.getUuid());
        lqw.eq(StringUtils.isNotBlank(bo.getKbUuid()), KnowledgeGraphSegment::getKbUuid, bo.getKbUuid());
        lqw.eq(StringUtils.isNotBlank(bo.getKbItemUuid()), KnowledgeGraphSegment::getKbItemUuid, bo.getKbItemUuid());
        lqw.eq(StringUtils.isNotBlank(bo.getDocUuid()), KnowledgeGraphSegment::getDocUuid, bo.getDocUuid());
        lqw.eq(StringUtils.isNotBlank(bo.getSegmentText()), KnowledgeGraphSegment::getSegmentText, bo.getSegmentText());
        lqw.eq(bo.getChunkIndex() != null, KnowledgeGraphSegment::getChunkIndex, bo.getChunkIndex());
        lqw.eq(bo.getTotalChunks() != null, KnowledgeGraphSegment::getTotalChunks, bo.getTotalChunks());
        lqw.eq(bo.getExtractionStatus() != null, KnowledgeGraphSegment::getExtractionStatus, bo.getExtractionStatus());
        lqw.eq(bo.getEntityCount() != null, KnowledgeGraphSegment::getEntityCount, bo.getEntityCount());
        lqw.eq(bo.getRelationCount() != null, KnowledgeGraphSegment::getRelationCount, bo.getRelationCount());
        lqw.eq(bo.getTokenUsed() != null, KnowledgeGraphSegment::getTokenUsed, bo.getTokenUsed());
        lqw.eq(StringUtils.isNotBlank(bo.getErrorMessage()), KnowledgeGraphSegment::getErrorMessage, bo.getErrorMessage());
        lqw.eq(bo.getUserId() != null, KnowledgeGraphSegment::getUserId, bo.getUserId());
        return lqw;
    }

    /**
     * 新增知识图谱片段
     *
     * @param bo 知识图谱片段
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(KnowledgeGraphSegmentBo bo) {
        KnowledgeGraphSegment add = MapstructUtils.convert(bo, KnowledgeGraphSegment.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识图谱片段
     *
     * @param bo 知识图谱片段
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(KnowledgeGraphSegmentBo bo) {
        KnowledgeGraphSegment update = MapstructUtils.convert(bo, KnowledgeGraphSegment.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeGraphSegment entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除知识图谱片段信息
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
}
