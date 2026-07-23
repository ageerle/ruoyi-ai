package org.ruoyi.common.trace.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.trace.domain.TraceNode;
import org.ruoyi.common.trace.domain.TraceRun;
import org.ruoyi.common.trace.domain.bo.TraceRunBo;
import org.ruoyi.common.trace.domain.vo.TraceDetailVo;
import org.ruoyi.common.trace.domain.vo.TraceNodeVo;
import org.ruoyi.common.trace.domain.vo.TraceRunVo;

import java.util.Date;
import java.util.List;

/**
 * 链路追踪记录服务。
 */
public interface TraceRecordService {

    void startRun(TraceRun run);

    void finishRun(String traceId, String status, String errorMessage, Date endTime, long durationMs);

    void startNode(TraceNode node);

    void finishNode(String traceId, String nodeId, String status, String errorMessage, String outputPayload, Date endTime, long durationMs);

    TableDataInfo<TraceRunVo> pageRuns(TraceRunBo bo, PageQuery pageQuery);

    TraceRunVo getRun(String traceId);

    List<TraceNodeVo> listNodes(String traceId);

    TraceDetailVo getDetail(String traceId);
}
