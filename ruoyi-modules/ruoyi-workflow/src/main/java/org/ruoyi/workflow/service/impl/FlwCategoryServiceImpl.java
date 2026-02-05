package org.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.*;
import org.ruoyi.common.mybatis.helper.DataBaseHelper;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.domain.FlowCategory;
import org.ruoyi.workflow.domain.bo.FlowCategoryBo;
import org.ruoyi.workflow.domain.vo.FlowCategoryVo;
import org.ruoyi.workflow.mapper.FlwCategoryMapper;
import org.ruoyi.workflow.service.IFlwCategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程分类Service业务层处理
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class FlwCategoryServiceImpl implements IFlwCategoryService, CategoryService {

    private final DefService defService;
    private final FlwCategoryMapper baseMapper;

    /**
     * 查询流程分类
     *
     * @param categoryId 主键
     * @return 流程分类
     */
    @Override
    public FlowCategoryVo queryById(Long categoryId) {
        return baseMapper.selectVoById(categoryId);
    }

    /**
     * 根据流程分类ID查询流程分类名称
     *
     * @param categoryId 流程分类ID
     * @return 流程分类名称
     */
    @Cacheable(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#categoryId")
    @Override
    public String selectCategoryNameById(Long categoryId) {
        if (ObjectUtil.isNull(categoryId)) {
            return null;
        }
        FlowCategory category = baseMapper.selectOne(new LambdaQueryWrapper<FlowCategory>()
            .select(FlowCategory::getCategoryName).eq(FlowCategory::getCategoryId, categoryId));
        return ObjectUtils.notNullGetter(category, FlowCategory::getCategoryName);
    }

    /**
     * 查询符合条件的流程分类列表
     *
     * @param bo 查询条件
     * @return 流程分类列表
     */
    @Override
    public List<FlowCategoryVo> queryList(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 查询流程分类树结构信息
     *
     * @param category 流程分类信息
     * @return 流程分类树信息集合
     */
    @Override
    public List<Tree<String>> selectCategoryTreeList(FlowCategoryBo category) {
        List<FlowCategoryVo> categoryList = this.queryList(category);
        if (CollUtil.isEmpty(categoryList)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.buildMultiRoot(
            categoryList,
            node -> Convert.toStr(node.getCategoryId()),
            node -> Convert.toStr(node.getParentId()),
            (node, treeNode) -> treeNode
                .setId(Convert.toStr(node.getCategoryId()))
                .setParentId(Convert.toStr(node.getParentId()))
                .setName(node.getCategoryName())
                .setWeight(node.getOrderNum())
        );
    }

    /**
     * 工作流查询分类
     *
     * @return 分类树结构列表
     */
    @Override
    public List<org.dromara.warm.flow.core.dto.Tree> queryCategory() {
        List<FlowCategoryVo> list = this.queryList(new FlowCategoryBo());
        return StreamUtils.toList(list, category -> new org.dromara.warm.flow.core.dto.Tree()
            .setId(Convert.toStr(category.getCategoryId()))
            .setName(category.getCategoryName())
            .setParentId(Convert.toStr(category.getParentId()))
        );
    }

    /**
     * 校验流程分类名称是否唯一
     *
     * @param category 流程分类信息
     * @return 结果
     */
    @Override
    public boolean checkCategoryNameUnique(FlowCategoryBo category) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getCategoryName, category.getCategoryName())
            .eq(FlowCategory::getParentId, category.getParentId())
            .ne(ObjectUtil.isNotNull(category.getCategoryId()), FlowCategory::getCategoryId, category.getCategoryId()));
        return !exist;
    }

    /**
     * 查询流程分类是否存在流程定义
     *
     * @param categoryId 流程分类ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkCategoryExistDefinition(Long categoryId) {
        FlowDefinition definition = new FlowDefinition();
        definition.setCategory(categoryId.toString());
        return defService.exists(definition);
    }

    /**
     * 是否存在流程分类子节点
     *
     * @param categoryId 流程分类ID
     * @return 结果
     */
    @Override
    public boolean hasChildByCategoryId(Long categoryId) {
        return baseMapper.exists(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getParentId, categoryId));
    }

    private LambdaQueryWrapper<FlowCategory> buildQueryWrapper(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = Wrappers.lambdaQuery();
        lqw.eq(FlowCategory::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getCategoryId()), FlowCategory::getCategoryId, bo.getCategoryId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), FlowCategory::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), FlowCategory::getCategoryName, bo.getCategoryName());
        lqw.orderByAsc(FlowCategory::getAncestors);
        lqw.orderByAsc(FlowCategory::getParentId);
        lqw.orderByAsc(FlowCategory::getOrderNum);
        lqw.orderByAsc(FlowCategory::getCategoryId);
        return lqw;
    }

    /**
     * 新增流程分类
     *
     * @param bo 流程分类
     * @return 是否新增成功
     */
    @Override
    public int insertByBo(FlowCategoryBo bo) {
        FlowCategory info = baseMapper.selectById(bo.getParentId());
        if (ObjectUtil.isNull(info)) {
            throw new ServiceException("父级流程分类不存在!");
        }
        FlowCategory category = MapstructUtils.convert(bo, FlowCategory.class);
        category.setAncestors(info.getAncestors() + StringUtils.SEPARATOR + category.getParentId());
        return baseMapper.insert(category);
    }

    /**
     * 修改流程分类
     *
     * @param bo 流程分类
     * @return 是否修改成功
     */
    @CacheEvict(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#bo.categoryId")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateByBo(FlowCategoryBo bo) {
        FlowCategory category = MapstructUtils.convert(bo, FlowCategory.class);
        FlowCategory oldCategory = baseMapper.selectById(category.getCategoryId());
        if (ObjectUtil.isNull(oldCategory)) {
            throw new ServiceException("流程分类不存在，无法修改");
        }
        if (!oldCategory.getParentId().equals(category.getParentId())) {
            FlowCategory newParentCategory = baseMapper.selectById(category.getParentId());
            if (ObjectUtil.isNotNull(newParentCategory)) {
                String newAncestors = newParentCategory.getAncestors() + StringUtils.SEPARATOR + newParentCategory.getCategoryId();
                String oldAncestors = oldCategory.getAncestors();
                category.setAncestors(newAncestors);
                updateCategoryChildren(category.getCategoryId(), newAncestors, oldAncestors);
            } else {
                throw new ServiceException("父级流程分类不存在!");
            }
        } else {
            category.setAncestors(oldCategory.getAncestors());
        }
        return baseMapper.updateById(category);
    }

    /**
     * 修改子元素关系
     *
     * @param categoryId   被修改的流程分类ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    private void updateCategoryChildren(Long categoryId, String newAncestors, String oldAncestors) {
        List<FlowCategory> children = baseMapper.selectList(new LambdaQueryWrapper<FlowCategory>()
            .apply(DataBaseHelper.findInSet(categoryId, "ancestors")));
        List<FlowCategory> list = new ArrayList<>();
        for (FlowCategory child : children) {
            FlowCategory category = new FlowCategory();
            category.setCategoryId(child.getCategoryId());
            category.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            list.add(category);
        }
        if (CollUtil.isNotEmpty(list)) {
            baseMapper.updateBatchById(list);
        }
    }

    /**
     * 删除流程分类信息
     *
     * @param categoryId 主键
     * @return 是否删除成功
     */
    @CacheEvict(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#categoryId")
    @Override
    public int deleteWithValidById(Long categoryId) {
        return baseMapper.deleteById(categoryId);
    }
}
