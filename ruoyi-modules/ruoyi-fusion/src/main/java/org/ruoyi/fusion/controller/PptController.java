package org.ruoyi.fusion.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.vo.ppt.*;
import org.ruoyi.system.service.IPptService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI_PPT
 *
 * @author NSL
 * @since 2024-12-30
 */
@Api(tags = "AI-PPT")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ppt")
public class PptController extends BaseController {

    private final IPptService pptService;

    @ApiOperation(value = "获取API Token")
    @GetMapping("/getApiToken")
    public R<String> getApiToken() {
        return R.ok(pptService.getApiToken());
    }

    @ApiOperation(value = "同步流式生成 PPT")
    @PostMapping("/syncStreamGeneratePpt")
    public R<Void> syncStreamGeneratePpt(String title) {
        pptService.syncStreamGeneratePpt(title);
        return R.ok();
    }

    @ApiOperation(value = "查询所有PPT列表")
    @PostMapping("/selectPptList")
    public R<Void> selectPptList(@RequestBody PptAllQueryDto pptQueryVo) {
        pptService.selectPptList(pptQueryVo);
        return R.ok();
    }

    @ApiOperation(value = "生成大纲")
    @PostMapping(value = "/generateOutline", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter generateOutline(@RequestBody PptGenerateOutlineDto generateOutlineDto) {
        return pptService.generateOutline(generateOutlineDto);
    }

    @ApiOperation(value = "生成大纲内容")
    @PostMapping(value = "/generateContent", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter generateOutline(@RequestBody PptGenerateContentDto generateContentDto) {
        return pptService.generateContent(generateContentDto);
    }

    @ApiOperation(value = "分页查询 PPT 模板")
    @PostMapping("/getTemplates")
    public R<JSONObject> getPptTemplates(@RequestBody PptTemplateQueryDto pptQueryVo) {
        return R.ok(pptService.getPptTemplates(pptQueryVo));
    }

    @ApiOperation(value = "生成 PPT")
    @PostMapping("/generatePptx")
    public R<JSONObject> generatePptx(@RequestBody PptGeneratePptxDto pptQueryVo) {
        return R.ok(pptService.generatePptx(pptQueryVo));
    }

    @ApiOperation(value = "生成PPT成功回调接口")
    @PostMapping("/successCallback")
    public R<Void> successCallback() {
        pptService.successCallback();
        return R.ok();
    }
}
