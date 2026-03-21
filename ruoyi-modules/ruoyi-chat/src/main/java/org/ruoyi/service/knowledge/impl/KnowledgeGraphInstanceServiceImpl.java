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
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphInstanceBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeGraphInstance;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphInstanceVo;
import org.ruoyi.mapper.knowledge.KnowledgeGraphInstanceMapper;
import org.ruoyi.service.knowledge.IKnowledgeGraphInstanceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识图谱实例Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeGraphInstanceServiceImpl implements IKnowledgeGraphInstanceService {

    private final KnowledgeGraphInstanceMapper baseMapper;

    /**
     * 查询知识图谱实例
     *
     * @param id 主键
     * @return 知识图谱实例
     */
    @Override
    public KnowledgeGraphInstanceVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询知识图谱实例列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识图谱实例分页列表
     */
    @Override
    public TableDataInfo<KnowledgeGraphInstanceVo> queryPageList(KnowledgeGraphInstanceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeGraphInstance> lqw = buildQueryWrapper(bo);
        Page<KnowledgeGraphInstanceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的知识图谱实例列表
     *
     * @param bo 查询条件
     * @return 知识图谱实例列表
     */
    @Override
    public List<KnowledgeGraphInstanceVo> queryList(KnowledgeGraphInstanceBo bo) {
        LambdaQueryWrapper<KnowledgeGraphInstance> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeGraphInstance> buildQueryWrapper(KnowledgeGraphInstanceBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeGraphInstance> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeGraphInstance::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getGraphUuid()), KnowledgeGraphInstance::getGraphUuid, bo.getGraphUuid());
        lqw.eq(StringUtils.isNotBlank(bo.getKnowledgeId()), KnowledgeGraphInstance::getKnowledgeId, bo.getKnowledgeId());
        lqw.like(StringUtils.isNotBlank(bo.getGraphName()), KnowledgeGraphInstance::getGraphName, bo.getGraphName());
        lqw.eq(bo.getGraphStatus() != null, KnowledgeGraphInstance::getGraphStatus, bo.getGraphStatus());
        lqw.eq(bo.getNodeCount() != null, KnowledgeGraphInstance::getNodeCount, bo.getNodeCount());
        lqw.eq(bo.getRelationshipCount() != null, KnowledgeGraphInstance::getRelationshipCount, bo.getRelationshipCount());
        lqw.eq(StringUtils.isNotBlank(bo.getConfig()), KnowledgeGraphInstance::getConfig, bo.getConfig());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), KnowledgeGraphInstance::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getEntityTypes()), KnowledgeGraphInstance::getEntityTypes, bo.getEntityTypes());
        lqw.eq(StringUtils.isNotBlank(bo.getRelationTypes()), KnowledgeGraphInstance::getRelationTypes, bo.getRelationTypes());
        lqw.eq(StringUtils.isNotBlank(bo.getErrorMessage()), KnowledgeGraphInstance::getErrorMessage, bo.getErrorMessage());
        return lqw;
    }

    /**
     * 新增知识图谱实例
     *
     * @param bo 知识图谱实例
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(KnowledgeGraphInstanceBo bo) {
        KnowledgeGraphInstance add = MapstructUtils.convert(bo, KnowledgeGraphInstance.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识图谱实例
     *
     * @param bo 知识图谱实例
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(KnowledgeGraphInstanceBo bo) {
        KnowledgeGraphInstance update = MapstructUtils.convert(bo, KnowledgeGraphInstance.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeGraphInstance entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除知识图谱实例信息
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
