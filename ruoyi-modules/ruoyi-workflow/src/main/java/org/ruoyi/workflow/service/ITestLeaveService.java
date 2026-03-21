package org.ruoyi.workflow.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.workflow.domain.bo.TestLeaveBo;
import org.ruoyi.workflow.domain.vo.TestLeaveVo;

import java.util.List;

/**
 * 请假Service接口
 *
 * @author may
 * @date 2023-07-21
 */
public interface ITestLeaveService {

    /**
     * 查询请假
     */
    TestLeaveVo queryById(Long id);

    /**
     * 查询请假列表
     */
    TableDataInfo<TestLeaveVo> queryPageList(TestLeaveBo bo, PageQuery pageQuery);

    /**
     * 查询请假列表
     */
    List<TestLeaveVo> queryList(TestLeaveBo bo);

    /**
     * 新增请假
     */
    TestLeaveVo insertByBo(TestLeaveBo bo);

    /**
     * 提交请假并发起流程
     */
    TestLeaveVo submitAndFlowStart(TestLeaveBo bo);

    /**
     * 修改请假
     */
    TestLeaveVo updateByBo(TestLeaveBo bo);

    /**
     * 校验并批量删除请假信息
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
