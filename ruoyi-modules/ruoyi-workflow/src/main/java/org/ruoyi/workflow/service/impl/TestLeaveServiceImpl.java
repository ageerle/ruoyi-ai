package org.ruoyi.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.dto.StartProcessDTO;
import org.ruoyi.common.core.domain.event.ProcessDeleteEvent;
import org.ruoyi.common.core.domain.event.ProcessEvent;
import org.ruoyi.common.core.domain.event.ProcessTaskEvent;
import org.ruoyi.common.core.enums.BusinessStatusEnum;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.WorkflowService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.domain.TestLeave;
import org.ruoyi.workflow.domain.bo.TestLeaveBo;
import org.ruoyi.workflow.domain.vo.TestLeaveVo;
import org.ruoyi.workflow.mapper.TestLeaveMapper;
import org.ruoyi.workflow.service.ITestLeaveService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 请假Service业务层处理
 *
 * @author may
 * @date 2023-07-21
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
@Slf4j
public class TestLeaveServiceImpl implements ITestLeaveService {

    private final TestLeaveMapper baseMapper;
    private final WorkflowService workflowService;

    /**
     * spel条件表达：判断小于2
     *
     * @param leaveDays 待判断的变量（可不传自行返回true或false）
     * @return boolean
     */
    public boolean eval(Integer leaveDays) {
        if (leaveDays <= 2) {
            return true;
        }
        return false;
    }

    /**
     * 查询请假
     */
    @Override
    public TestLeaveVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询请假列表
     */
    @Override
    public TableDataInfo<TestLeaveVo> queryPageList(TestLeaveBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestLeave> lqw = buildQueryWrapper(bo);
        Page<TestLeaveVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询请假列表
     */
    @Override
    public List<TestLeaveVo> queryList(TestLeaveBo bo) {
        LambdaQueryWrapper<TestLeave> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<TestLeave> buildQueryWrapper(TestLeaveBo bo) {
        LambdaQueryWrapper<TestLeave> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getLeaveType()), TestLeave::getLeaveType, bo.getLeaveType());
        lqw.ge(bo.getStartLeaveDays() != null, TestLeave::getLeaveDays, bo.getStartLeaveDays());
        lqw.le(bo.getEndLeaveDays() != null, TestLeave::getLeaveDays, bo.getEndLeaveDays());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 新增请假
     */
    @Override
    public TestLeaveVo insertByBo(TestLeaveBo bo) {
        long day = DateUtil.betweenDay(bo.getStartDate(), bo.getEndDate(), true);
        // 截止日期也算一天
        bo.setLeaveDays((int) day + 1);
        bo.setApplyCode(System.currentTimeMillis() + StrUtil.EMPTY);
        TestLeave add = MapstructUtils.convert(bo, TestLeave.class);
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return MapstructUtils.convert(add, TestLeaveVo.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TestLeaveVo submitAndFlowStart(TestLeaveBo bo) {
        long day = DateUtil.betweenDay(bo.getStartDate(), bo.getEndDate(), true);
        // 截止日期也算一天
        bo.setLeaveDays((int) day + 1);
        if (ObjectUtil.isNull(bo.getId())) {
            bo.setApplyCode(System.currentTimeMillis() + StrUtil.EMPTY);
        }
        TestLeave leave = MapstructUtils.convert(bo, TestLeave.class);
        boolean flag = baseMapper.insertOrUpdate(leave);
        if (flag) {
            bo.setId(leave.getId());
            // 后端发起需要忽略权限
            bo.getParams().put("ignore", true);

            StartProcessDTO startProcess = new StartProcessDTO();
            startProcess.setBusinessId(leave.getId().toString());
            startProcess.setFlowCode(StringUtils.isEmpty(bo.getFlowCode()) ? "leave1" : bo.getFlowCode());
            startProcess.setVariables(bo.getParams());
            // 后端发起 如果没有登录用户 比如定时任务 可以手动设置一个处理人id
            // startProcess.setHandler("0");

            boolean flag1 = workflowService.startCompleteTask(startProcess);
            if (!flag1) {
                throw new ServiceException("流程发起异常");
            }
        }
        return MapstructUtils.convert(leave, TestLeaveVo.class);
    }

    /**
     * 修改请假
     */
    @Override
    public TestLeaveVo updateByBo(TestLeaveBo bo) {
        TestLeave update = MapstructUtils.convert(bo, TestLeave.class);
        baseMapper.updateById(update);
        return MapstructUtils.convert(update, TestLeaveVo.class);
    }

    /**
     * 批量删除请假
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(List<Long> ids) {
        workflowService.deleteInstance(ids);
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 总体流程监听(例如: 草稿，撤销，退回，作废，终止，已完成等)
     * 正常使用只需#processEvent.flowCode=='leave1'
     * 示例为了方便则使用startsWith匹配了全部示例key
     *
     * @param processEvent 参数
     */
    @EventListener(condition = "#processEvent.flowCode.startsWith('leave')")
    public void processHandler(ProcessEvent processEvent) {
        log.info("当前任务执行了{}", processEvent.toString());
        TestLeave testLeave = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
        testLeave.setStatus(processEvent.getStatus());
        // 用于例如审批附件 审批意见等 存储到业务表内 自行根据业务实现存储流程
        Map<String, Object> params = processEvent.getParams();
        if (MapUtil.isNotEmpty(params)) {
            // 历史任务扩展(通常为附件)
            String hisTaskExt = Convert.toStr(params.get("hisTaskExt"));
            // 办理人
            String handler = Convert.toStr(params.get("handler"));
            // 办理意见
            String message = Convert.toStr(params.get("message"));
        }
        if (processEvent.getSubmit()) {
            if (StringUtils.isBlank(testLeave.getApplyCode())) {
                String businessCode = MapUtil.getStr(params, FlowConstant.BUSINESS_CODE, StrUtil.EMPTY);
                testLeave.setApplyCode(businessCode);
            }
            testLeave.setStatus(BusinessStatusEnum.WAITING.getStatus());
        }
        baseMapper.updateById(testLeave);
    }

    /**
     * 执行任务创建监听(也代表上一条任务完成事件)
     * 示例：也可通过  @EventListener(condition = "#processTaskEvent.flowCode=='leave1'")进行判断
     * 在方法中判断流程节点key
     * if ("xxx".equals(processTaskEvent.getNodeCode())) {
     * //执行业务逻辑
     * }
     *
     * @param processTaskEvent 参数
     */
    @EventListener(condition = "#processTaskEvent.flowCode.startsWith('leave')")
    public void processTaskHandler(ProcessTaskEvent processTaskEvent) {
        log.info("当前任务创建了{}", processTaskEvent.toString());
    }

    /**
     * 监听删除流程事件
     * 正常使用只需#processDeleteEvent.flowCode=='leave1'
     * 示例为了方便则使用startsWith匹配了全部示例key
     *
     * @param processDeleteEvent 参数
     */
    @EventListener(condition = "#processDeleteEvent.flowCode.startsWith('leave')")
    public void processDeleteHandler(ProcessDeleteEvent processDeleteEvent) {
        log.info("监听删除流程事件，当前任务执行了{}", processDeleteEvent.toString());
        TestLeave testLeave = baseMapper.selectById(Convert.toLong(processDeleteEvent.getBusinessId()));
        if (ObjectUtil.isNull(testLeave)) {
            return;
        }
        baseMapper.deleteById(testLeave.getId());
    }

}
