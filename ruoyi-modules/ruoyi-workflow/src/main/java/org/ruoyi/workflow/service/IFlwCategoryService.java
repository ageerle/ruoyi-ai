package org.ruoyi.workflow.service;

import cn.hutool.core.lang.tree.Tree;
import org.ruoyi.workflow.domain.bo.FlowCategoryBo;
import org.ruoyi.workflow.domain.vo.FlowCategoryVo;

import java.util.List;

/**
 * 流程分类Service接口
 *
 * @author may
 */
public interface IFlwCategoryService {

    /**
     * 查询流程分类
     *
     * @param categoryId 主键
     * @return 流程分类
     */
    FlowCategoryVo queryById(Long categoryId);

    /**
     * 根据流程分类ID查询流程分类名称
     *
     * @param categoryId 流程分类ID
     * @return 流程分类名称
     */
    String selectCategoryNameById(Long categoryId);

    /**
     * 查询符合条件的流程分类列表
     *
     * @param bo 查询条件
     * @return 流程分类列表
     */
    List<FlowCategoryVo> queryList(FlowCategoryBo bo);

    /**
     * 查询流程分类树结构信息
     *
     * @param category 流程分类信息
     * @return 流程分类树信息集合
     */
    List<Tree<String>> selectCategoryTreeList(FlowCategoryBo category);

    /**
     * 校验流程分类名称是否唯一
     *
     * @param category 流程分类信息
     * @return 结果
     */
    boolean checkCategoryNameUnique(FlowCategoryBo category);

    /**
     * 查询流程分类是否存在流程定义
     *
     * @param categoryId 流程分类ID
     * @return 结果 true 存在 false 不存在
     */
    boolean checkCategoryExistDefinition(Long categoryId);

    /**
     * 是否存在流程分类子节点
     *
     * @param categoryId 流程分类ID
     * @return 结果
     */
    boolean hasChildByCategoryId(Long categoryId);

    /**
     * 新增流程分类
     *
     * @param bo 流程分类
     * @return 是否新增成功
     */
    int insertByBo(FlowCategoryBo bo);

    /**
     * 修改流程分类
     *
     * @param bo 流程分类
     * @return 是否修改成功
     */
    int updateByBo(FlowCategoryBo bo);

    /**
     * 删除流程分类信息
     *
     * @param categoryId 主键
     * @return 是否删除成功
     */
    int deleteWithValidById(Long categoryId);
}
