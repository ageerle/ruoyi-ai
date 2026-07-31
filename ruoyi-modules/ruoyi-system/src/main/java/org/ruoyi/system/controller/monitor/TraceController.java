package org.ruoyi.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.trace.domain.bo.TraceRunBo;
import org.ruoyi.common.trace.domain.vo.TraceDetailVo;
import org.ruoyi.common.trace.domain.vo.TraceNodeVo;
import org.ruoyi.common.trace.domain.vo.TraceRunVo;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 链路追踪监控
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/trace")
public class TraceController extends BaseController {

    private final TraceRecordService traceRecordService;

    /**
     * 获取链路追踪运行列表
     */
    @SaCheckPermission("monitor:trace:list")
    @GetMapping("/run/list")
    public TableDataInfo<TraceRunVo> list(TraceRunBo bo, PageQuery pageQuery) {
        return traceRecordService.pageRuns(bo, pageQuery);
    }

    /**
     * 获取链路追踪运行详情
     */
    @SaCheckPermission("monitor:trace:query")
    @GetMapping("/run/{traceId}")
    public R<TraceRunVo> run(@PathVariable String traceId) {
        return R.ok(traceRecordService.getRun(traceId));
    }

    /**
     * 获取链路追踪节点列表
     */
    @SaCheckPermission("monitor:trace:query")
    @GetMapping("/node/list/{traceId}")
    public R<List<TraceNodeVo>> nodes(@PathVariable String traceId) {
        return R.ok(traceRecordService.listNodes(traceId));
    }

    /**
     * 获取链路追踪完整详情
     */
    @SaCheckPermission("monitor:trace:query")
    @GetMapping("/detail/{traceId}")
    public R<TraceDetailVo> detail(@PathVariable String traceId) {
        return R.ok(traceRecordService.getDetail(traceId));
    }
}
