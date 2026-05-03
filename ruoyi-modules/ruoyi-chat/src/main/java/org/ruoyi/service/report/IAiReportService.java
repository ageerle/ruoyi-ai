package org.ruoyi.service.report;

import org.ruoyi.domain.dto.request.AiReportGenerateRequest;
import org.ruoyi.domain.dto.request.AiReportExecuteRequest;
import org.ruoyi.domain.dto.request.AiReportRefineRequest;
import org.ruoyi.domain.dto.response.AiReportResponse;

public interface IAiReportService {

    AiReportResponse generate(AiReportGenerateRequest request);

    AiReportResponse execute(AiReportExecuteRequest request);

    AiReportResponse refine(AiReportRefineRequest request);
}
