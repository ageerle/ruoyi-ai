package org.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.common.core.domain.dto.TaskAssigneeDTO;
import org.ruoyi.common.core.domain.model.TaskAssigneeBody;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.FlowSpel;
import org.ruoyi.workflow.domain.bo.FlowSpelBo;
import org.ruoyi.workflow.domain.vo.FlowSpelVo;
import org.ruoyi.workflow.mapper.FlwSpelMapper;
import org.ruoyi.workflow.service.IFlwSpelService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 流程spel达式定义Service业务层处理
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwSpelServiceImpl implements IFlwSpelService {

    private final FlwSpelMapper baseMapper;

    /**
     * 查询流程spel达式定义
     *
     * @param id 主键
     * @return 流程spel达式定义
     */
    @Override
    public FlowSpelVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询流程spel达式定义列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 流程spel达式定义分页列表
     */
    @Override
    public TableDataInfo<FlowSpelVo> queryPageList(FlowSpelBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowSpel> lqw = buildQueryWrapper(bo);
        Page<FlowSpelVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的流程spel达式定义列表
     *
     * @param bo 查询条件
     * @return 流程spel达式定义列表
     */
    @Override
    public List<FlowSpelVo> queryList(FlowSpelBo bo) {
        LambdaQueryWrapper<FlowSpel> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<FlowSpel> buildQueryWrapper(FlowSpelBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<FlowSpel> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(FlowSpel::getId);
        lqw.like(StringUtils.isNotBlank(bo.getComponentName()), FlowSpel::getComponentName, bo.getComponentName());
        lqw.like(StringUtils.isNotBlank(bo.getMethodName()), FlowSpel::getMethodName, bo.getMethodName());
        lqw.eq(StringUtils.isNotBlank(bo.getMethodParams()), FlowSpel::getMethodParams, bo.getMethodParams());
        lqw.eq(StringUtils.isNotBlank(bo.getViewSpel()), FlowSpel::getViewSpel, bo.getViewSpel());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), FlowSpel::getStatus, bo.getStatus());
        lqw.like(StringUtils.isNotBlank(bo.getRemark()), FlowSpel::getRemark, bo.getRemark());
        return lqw;
    }

    /**
     * 新增流程spel达式定义
     *
     * @param bo 流程spel达式定义
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(FlowSpelBo bo) {
        FlowSpel add = MapstructUtils.convert(bo, FlowSpel.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改流程spel达式定义
     *
     * @param bo 流程spel达式定义
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(FlowSpelBo bo) {
        FlowSpel update = MapstructUtils.convert(bo, FlowSpel.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(FlowSpel entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除流程spel达式定义信息
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
     * 查询spel并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    @Override
    public TaskAssigneeDTO selectSpelByTaskAssigneeList(TaskAssigneeBody taskQuery) {
        PageQuery pageQuery = new PageQuery(taskQuery.getPageSize(), taskQuery.getPageNum());
        FlowSpelBo bo = new FlowSpelBo();
        bo.setViewSpel(taskQuery.getHandlerCode());
        bo.setRemark(taskQuery.getHandlerName());
        bo.setStatus(SystemConstants.NORMAL);
        Map<String, Object> params = bo.getParams();
        params.put("beginTime", taskQuery.getBeginTime());
        params.put("endTime", taskQuery.getEndTime());
        TableDataInfo<FlowSpelVo> page = this.queryPageList(bo, pageQuery);
        // 使用封装的字段映射方法进行转换
        List<TaskAssigneeDTO.TaskHandler> handlers = TaskAssigneeDTO.convertToHandlerList(page.getRows(),
            FlowSpelVo::getViewSpel, item -> "", FlowSpelVo::getRemark, item -> "", FlowSpelVo::getCreateTime);
        return new TaskAssigneeDTO(page.getTotal(), handlers);
    }

    /**
     * 根据视图 SpEL 表达式列表，查询对应的备注信息
     *
     * @param viewSpels SpEL 表达式列表
     * @return 映射表：key 为 SpEL 表达式，value 为对应备注；若为空则返回空 Map
     */
    @Override
    public Map<String, String> selectRemarksBySpels(List<String> viewSpels) {
        if (CollUtil.isEmpty(viewSpels)) {
            return Collections.emptyMap();
        }
        List<FlowSpel> list = baseMapper.selectList(
            new LambdaQueryWrapper<FlowSpel>()
                .select(FlowSpel::getViewSpel, FlowSpel::getRemark)
                .in(FlowSpel::getViewSpel, viewSpels)
        );
        return StreamUtils.toMap(list, FlowSpel::getViewSpel, x ->
            StringUtils.isEmpty(x.getRemark()) ? "" : x.getRemark()
        );
    }

}
