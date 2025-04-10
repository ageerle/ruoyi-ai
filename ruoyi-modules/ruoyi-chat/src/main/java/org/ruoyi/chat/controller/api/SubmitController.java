package org.ruoyi.chat.controller.api;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.chat.domain.dto.*;
import org.ruoyi.chat.enums.ActionType;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.util.MjOkHttpUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Api(tags = "任务提交")
@RestController
@RequestMapping("/mj/submit")
@RequiredArgsConstructor
@Slf4j
public class SubmitController {

    private final IChatCostService chatCostService;
    private final MjOkHttpUtil mjOkHttpUtil;

    @ApiOperation(value = "绘图变化")
    @PostMapping("/change")
    public String change(@RequestBody SubmitChangeDTO changeDTO) {
        String jsonStr = JSONUtil.toJsonStr(changeDTO);
        String url = "mj/submit/change";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "执行动作")
    @PostMapping("/action")
    public String action(@RequestBody SubmitActionDTO changeDTO) {
        ActionType actionType = ActionType.fromCustomId(getAction(changeDTO.getCustomId()));
        Optional.ofNullable(actionType).ifPresentOrElse(
            type -> {
                switch (type) {
                    case UP_SAMPLE:
                        chatCostService.taskDeduct("mj","enlarge", NumberUtils.toDouble(mjOkHttpUtil.getKey("upsample"), 0.3));
                        break;
                    case IN_PAINT:
                        // 局部重绘已经扣费,不执行任何操作
                        break;
                    default:
                        chatCostService.taskDeduct("mj","change", NumberUtils.toDouble(mjOkHttpUtil.getKey("change"), 0.3));
                        break;
                }
            },
            () -> chatCostService.taskDeduct("mj","change", NumberUtils.toDouble(mjOkHttpUtil.getKey("change"), 0.3))
        );

        String jsonStr = JSONUtil.toJsonStr(changeDTO);
        String url = "mj/submit/action";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "绘图变化-simple")
    @PostMapping("/simple-change")
    public String simpleChange(@RequestBody SubmitSimpleChangeDTO simpleChangeDTO) {
        String jsonStr = JSONUtil.toJsonStr(simpleChangeDTO);
        String url = "mj/submit/simple-change";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "提交图生图、混图任务")
    @PostMapping("/blend")
    public String blend(@RequestBody SubmitBlendDTO blendDTO) {
        chatCostService.taskDeduct("mj","blend", NumberUtils.toDouble(mjOkHttpUtil.getKey("blend"), 0.3));
        String jsonStr = JSONUtil.toJsonStr(blendDTO);
        String url = "mj/submit/blend";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "提交图生文任务")
    @PostMapping("/describe")
    public String describe(@RequestBody SubmitDescribeDTO describeDTO) {
        chatCostService.taskDeduct("mj","describe", NumberUtils.toDouble(mjOkHttpUtil.getKey("describe"), 0.1));
        String jsonStr = JSONUtil.toJsonStr(describeDTO);
        String url = "mj/submit/describe";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "提交文生图任务")
    @PostMapping("/imagine")
    public String imagine(@RequestBody SubmitImagineDTO imagineDTO) {
        chatCostService.taskDeduct("mj",imagineDTO.getPrompt(), NumberUtils.toDouble(mjOkHttpUtil.getKey("imagine"), 0.3));
        String jsonStr = JSONUtil.toJsonStr(imagineDTO);
        String url = "mj/submit/imagine";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "提交局部重绘任务")
    @PostMapping("/modal")
    public String modal(@RequestBody SubmitModalDTO submitModalDTO) {
        chatCostService.taskDeduct("mj","repaint ", NumberUtils.toDouble(mjOkHttpUtil.getKey("inpaint"), 0.1));
        String jsonStr = JSONUtil.toJsonStr(submitModalDTO);
        String url = "mj/submit/modal";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "提交提示词分析任务")
    @PostMapping("/shorten")
    public String shorten(@RequestBody SubmitShortenDTO submitShortenDTO) {
        chatCostService.taskDeduct("mj","shorten", NumberUtils.toDouble(mjOkHttpUtil.getKey("shorten"), 0.1));
        String jsonStr = JSONUtil.toJsonStr(submitShortenDTO);
        String url = "mj/submit/shorten";
        Request request = mjOkHttpUtil.createPostRequest(url, jsonStr);
        return mjOkHttpUtil.executeRequest(request);
    }

    public String getAction(String customId) {
        if (customId == null || customId.isEmpty()) {
            return null;
        }
        String[] parts = customId.split("::");
        return customId.endsWith("SOLO") ? parts[1] : parts[2];
    }

}
