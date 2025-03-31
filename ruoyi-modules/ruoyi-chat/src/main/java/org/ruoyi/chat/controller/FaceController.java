package org.ruoyi.chat.controller;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.chat.domain.InsightFace;
import org.ruoyi.chat.util.MjOkHttpUtil;
import org.ruoyi.system.service.IChatCostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Api(tags = "任务查询")
@RestController
@RequestMapping("/mj")
@RequiredArgsConstructor
@Slf4j
public class FaceController {

    private final IChatCostService chatCostService;

    private final MjOkHttpUtil mjOkHttpUtil;

    @ApiOperation(value = "换脸")
    @PostMapping("/insight-face/swap")
    public String insightFace(@RequestBody InsightFace insightFace) {
        // 扣除接口费用并且保存消息记录
        chatCostService.taskDeduct("mj","Face Changing", NumberUtils.toDouble(mjOkHttpUtil.getKey("faceSwapping"), 0.1));
        // 创建请求体（这里使用JSON作为媒体类型）
        String insightFaceJson = JSONUtil.toJsonStr(insightFace);
        String url = "mj/insight-face/swap";
        Request request = mjOkHttpUtil.createPostRequest(url, insightFaceJson);
        return mjOkHttpUtil.executeRequest(request);
    }

}
