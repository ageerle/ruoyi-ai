package org.ruoyi.common.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.trace.constant.TraceDisplayConstants;
import org.ruoyi.common.trace.domain.TraceNode;
import org.ruoyi.common.trace.domain.TraceRun;
import org.ruoyi.common.trace.domain.bo.TraceRunBo;
import org.ruoyi.common.trace.domain.vo.TraceDetailVo;
import org.ruoyi.common.trace.domain.vo.TraceNodeVo;
import org.ruoyi.common.trace.domain.vo.TraceRunVo;
import org.ruoyi.common.trace.mapper.TraceNodeMapper;
import org.ruoyi.common.trace.mapper.TraceRunMapper;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.common.trace.util.TracePayloadUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 链路追踪记录服务实现。
 * <p>
 * 在自动映射的基础上，对 VO 进行二次加工：补充展示标签、解析 payload 为结构化对象、计算统计信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceRecordServiceImpl implements TraceRecordService {

    private static final int TOP_SLOW_NODES_LIMIT = 5;

    private final TraceRunMapper traceRunMapper;
    private final TraceNodeMapper traceNodeMapper;

    @Override
    public void startRun(TraceRun run) {
        try {
            traceRunMapper.insert(run);
        } catch (Exception e) {
            log.warn("写入 trace run 失败，traceId={}", run == null ? null : run.getTraceId(), e);
        }
    }

    @Override
    public void finishRun(String traceId, String status, String errorMessage, Date endTime, long durationMs) {
        try {
            TraceRun update = new TraceRun();
            update.setStatus(status);
            update.setErrorMessage(errorMessage);
            update.setEndTime(endTime);
            update.setDurationMs(durationMs);
            traceRunMapper.update(update, Wrappers.lambdaUpdate(TraceRun.class).eq(TraceRun::getTraceId, traceId));
        } catch (Exception e) {
            log.warn("更新 trace run 失败，traceId={}", traceId, e);
        }
    }

    @Override
    public void startNode(TraceNode node) {
        try {
            traceNodeMapper.insert(node);
        } catch (Exception e) {
            log.warn("写入 trace node 失败，traceId={}, nodeId={}",
                node == null ? null : node.getTraceId(), node == null ? null : node.getNodeId(), e);
        }
    }

    @Override
    public void finishNode(String traceId, String nodeId, String status, String errorMessage, String outputPayload, Date endTime, long durationMs) {
        try {
            TraceNode update = new TraceNode();
            update.setStatus(status);
            update.setErrorMessage(errorMessage);
            update.setOutputPayload(outputPayload);
            update.setEndTime(endTime);
            update.setDurationMs(durationMs);
            traceNodeMapper.update(update, Wrappers.lambdaUpdate(TraceNode.class)
                .eq(TraceNode::getTraceId, traceId)
                .eq(TraceNode::getNodeId, nodeId));
        } catch (Exception e) {
            log.warn("更新 trace node 失败，traceId={}, nodeId={}", traceId, nodeId, e);
        }
    }

    @Override
    public TableDataInfo<TraceRunVo> pageRuns(TraceRunBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TraceRun> wrapper = buildRunWrapper(bo == null ? new TraceRunBo() : bo);
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            wrapper.orderByDesc(TraceRun::getStartTime);
        }
        Page<TraceRunVo> page = traceRunMapper.selectVoPage(pageQuery.build(), wrapper);
        if (page.getRecords() != null) {
            page.getRecords().forEach(this::enrichRunVo);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public TraceRunVo getRun(String traceId) {
        TraceRunVo run = traceRunMapper.selectVoOne(Wrappers.lambdaQuery(TraceRun.class).eq(TraceRun::getTraceId, traceId));
        if (run != null) {
            enrichRunVo(run);
        }
        return run;
    }

    @Override
    public List<TraceNodeVo> listNodes(String traceId) {
        List<TraceNodeVo> nodes = traceNodeMapper.selectVoList(Wrappers.lambdaQuery(TraceNode.class)
            .eq(TraceNode::getTraceId, traceId)
            .orderByAsc(TraceNode::getStartTime)
            .orderByAsc(TraceNode::getId));
        if (nodes != null) {
            nodes.forEach(this::enrichNodeVo);
        }
        return nodes;
    }

    @Override
    public TraceDetailVo getDetail(String traceId) {
        TraceDetailVo detail = new TraceDetailVo();
        TraceRunVo run = getRun(traceId);
        detail.setRun(run);

        List<TraceNodeVo> nodes = listNodes(traceId);
        // 返回扁平列表，前端自行按 parentNodeId 建树
        detail.setNodes(nodes != null ? nodes : new ArrayList<>());

        // 计算统计信息
        if (nodes != null) {
            detail.setStatistics(buildStatistics(nodes, run));
        }
        return detail;
    }

    // ======================== VO 加工 ========================

    /**
     * 为 TraceRunVo 补充展示标签和解析后的 metadata。
     */
    private void enrichRunVo(TraceRunVo vo) {
        if (vo == null) {
            return;
        }
        vo.setStatusLabel(TraceDisplayConstants.statusLabel(vo.getStatus()));
        vo.setBusinessTypeLabel(TraceDisplayConstants.businessTypeLabel(vo.getBusinessType()));
        vo.setParsedMetadata(TracePayloadUtils.parseJsonToMap(vo.getMetadata()));
    }

    /**
     * 为 TraceNodeVo 补充展示标签和解析后的 payload。
     */
    private void enrichNodeVo(TraceNodeVo vo) {
        if (vo == null) {
            return;
        }
        vo.setNodeTypeLabel(TraceDisplayConstants.nodeTypeLabel(vo.getNodeType()));
        vo.setStatusLabel(TraceDisplayConstants.statusLabel(vo.getStatus()));
        vo.setNodeDisplayName(TraceDisplayConstants.prettifyNodeName(vo.getNodeName()));
        vo.setParsedInput(TracePayloadUtils.parseJsonToMap(vo.getInputPayload()));
        vo.setParsedOutput(TracePayloadUtils.parseJsonToMap(vo.getOutputPayload()));
        vo.setParsedMetadata(TracePayloadUtils.parseJsonToMap(vo.getMetadata()));
    }

    // ======================== 统计计算 ========================
    private TraceDetailVo.TraceStatistics buildStatistics(List<TraceNodeVo> nodes, TraceRunVo run) {
        TraceDetailVo.TraceStatistics stats = new TraceDetailVo.TraceStatistics();

        int total = nodes == null ? 0 : nodes.size();
        int success = 0;
        int failed = 0;
        int running = 0;
        int maxDepth = 0;
        long totalDuration = 0;

        for (TraceNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (TraceDisplayConstants.isSuccess(node.getStatus())) {
                success++;
            } else if (TraceDisplayConstants.isFailed(node.getStatus())) {
                failed++;
            } else if (TraceDisplayConstants.isRunning(node.getStatus())) {
                running++;
            }

            int depth = node.getDepth() == null ? 0 : node.getDepth();
            if (depth > maxDepth) {
                maxDepth = depth;
            }

            long dur = node.getDurationMs() == null ? 0 : node.getDurationMs();
            totalDuration += dur;
        }

        stats.setTotalNodes(total);
        stats.setSuccessCount(success);
        stats.setFailedCount(failed);
        stats.setRunningCount(running);
        stats.setMaxDepth(maxDepth);
        stats.setAvgDurationMs(total > 0 ? totalDuration / total : 0);
        stats.setTotalDurationMs(run != null && run.getDurationMs() != null ? run.getDurationMs() : totalDuration);

        // 慢节点 Top N
        long totalMs = stats.getTotalDurationMs();
        List<TraceDetailVo.SlowNodeInfo> topSlow = nodes.stream()
            .filter(n -> n != null && n.getDurationMs() != null && n.getDurationMs() > 0)
            .sorted(Comparator.comparingLong(TraceNodeVo::getDurationMs).reversed())
            .limit(TOP_SLOW_NODES_LIMIT)
            .map(n -> {
                TraceDetailVo.SlowNodeInfo info = new TraceDetailVo.SlowNodeInfo();
                info.setNodeId(n.getNodeId());
                info.setNodeDisplayName(n.getNodeDisplayName() != null ? n.getNodeDisplayName() : n.getNodeName());
                info.setNodeTypeLabel(n.getNodeTypeLabel());
                info.setDurationMs(n.getDurationMs());
                info.setPercentOfTotal(totalMs > 0 ? Math.round(n.getDurationMs() * 1000.0 / totalMs) / 10.0 : 0);
                return info;
            })
            .collect(Collectors.toList());
        stats.setTopSlowNodes(topSlow);

        return stats;
    }

    // ======================== 查询辅助 ========================

    private LambdaQueryWrapper<TraceRun> buildRunWrapper(TraceRunBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<TraceRun>()
            .eq(StringUtils.isNotBlank(bo.getTraceId()), TraceRun::getTraceId, bo.getTraceId())
            .like(StringUtils.isNotBlank(bo.getTraceName()), TraceRun::getTraceName, bo.getTraceName())
            .eq(StringUtils.isNotBlank(bo.getBusinessType()), TraceRun::getBusinessType, bo.getBusinessType())
            .eq(StringUtils.isNotBlank(bo.getBusinessId()), TraceRun::getBusinessId, bo.getBusinessId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), TraceRun::getStatus, bo.getStatus())
            .eq(bo.getUserId() != null, TraceRun::getUserId, bo.getUserId())
            .eq(StringUtils.isNotBlank(bo.getTenantId()), TraceRun::getTenantId, bo.getTenantId())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                TraceRun::getStartTime, params.get("beginTime"), params.get("endTime"));
    }

}
