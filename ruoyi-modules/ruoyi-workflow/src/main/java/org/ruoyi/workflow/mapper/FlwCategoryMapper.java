package org.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.common.mybatis.helper.DataBaseHelper;
import org.ruoyi.workflow.domain.FlowCategory;
import org.ruoyi.workflow.domain.vo.FlowCategoryVo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 流程分类Mapper接口
 *
 * @author may
 * @date 2023-06-27
 */
public interface FlwCategoryMapper extends BaseMapperPlus<FlowCategory, FlowCategoryVo> {

    /**
     * 根据父流程分类ID查询其所有子流程分类的列表
     *
     * @param parentId 父流程分类ID
     * @return 包含子流程分类的列表
     */
    default List<FlowCategory> selectListByParentId(Long parentId) {
        return this.selectList(new LambdaQueryWrapper<FlowCategory>()
            .select(FlowCategory::getCategoryId)
            .apply(DataBaseHelper.findInSet(parentId, "ancestors")));
    }

    /**
     * 根据父流程分类ID查询包括父ID及其所有子流程分类ID的列表
     *
     * @param parentId 父流程分类ID
     * @return 包含父ID和子流程分类ID的列表
     */
    default List<Long> selectCategoryIdsByParentId(Long parentId) {
        return Stream.concat(
            this.selectListByParentId(parentId).stream()
                .map(FlowCategory::getCategoryId),
            Stream.of(parentId)
        ).collect(Collectors.toList());
    }

}
