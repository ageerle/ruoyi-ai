package org.ruoyi.controller.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.domain.dto.request.AiReportExecuteRequest;
import org.ruoyi.domain.dto.request.AiReportGenerateRequest;
import org.ruoyi.domain.dto.request.AiReportRefineRequest;
import org.ruoyi.domain.dto.response.AiReportResponse;
import org.ruoyi.service.report.IAiReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/report")
public class AiReportController {

    private final IAiReportService aiReportService;

    @PostMapping("/generate")
    public R<AiReportResponse> generate(@RequestBody @Valid AiReportGenerateRequest request) {
        return R.ok(aiReportService.generate(request));
    }

    @PostMapping("/execute")
    public R<AiReportResponse> execute(@RequestBody @Valid AiReportExecuteRequest request) {
        return R.ok(aiReportService.execute(request));
    }

    @PostMapping("/refine")
    public R<AiReportResponse> refine(@RequestBody @Valid AiReportRefineRequest request) {
        return R.ok(aiReportService.refine(request));
    }
}
