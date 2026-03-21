package org.ruoyi.workflow.service;

import org.ruoyi.common.core.domain.dto.TaskAssigneeDTO;
import org.ruoyi.common.core.domain.model.TaskAssigneeBody;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.workflow.domain.bo.FlowSpelBo;
import org.ruoyi.workflow.domain.vo.FlowSpelVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程spel达式定义Service接口
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
public interface IFlwSpelService {

    /**
     * 查询流程spel达式定义
     *
     * @param id 主键
     * @return 流程spel达式定义
     */
    FlowSpelVo queryById(Long id);

    /**
     * 分页查询流程spel达式定义列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 流程spel达式定义分页列表
     */
    TableDataInfo<FlowSpelVo> queryPageList(FlowSpelBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的流程spel达式定义列表
     *
     * @param bo 查询条件
     * @return 流程spel达式定义列表
     */
    List<FlowSpelVo> queryList(FlowSpelBo bo);

    /**
     * 新增流程spel达式定义
     *
     * @param bo 流程spel达式定义
     * @return 是否新增成功
     */
    Boolean insertByBo(FlowSpelBo bo);

    /**
     * 修改流程spel达式定义
     *
     * @param bo 流程spel达式定义
     * @return 是否修改成功
     */
    Boolean updateByBo(FlowSpelBo bo);

    /**
     * 校验并批量删除流程spel达式定义信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询spel并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    TaskAssigneeDTO selectSpelByTaskAssigneeList(TaskAssigneeBody taskQuery);

    /**
     * 根据视图 SpEL 表达式列表，查询对应的备注信息
     *
     * @param viewSpels SpEL 表达式列表
     * @return 映射表：key 为 SpEL 表达式，value 为对应备注；若为空则返回空 Map
     */
    Map<String, String> selectRemarksBySpels(List<String> viewSpels);

}
