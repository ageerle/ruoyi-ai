package com.xmzs.midjourney.controller;

import cn.hutool.json.JSONUtil;
import com.xmzs.midjourney.domain.InsightFace;
import com.xmzs.midjourney.domain.MjPriceConfig;
import com.xmzs.midjourney.util.MjOkHttpUtil;

import com.xmzs.system.service.IChatCostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
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

    private final MjPriceConfig priceConfig;

    @ApiOperation(value = "换脸")
    @PostMapping("/insight-face/swap")
    public String insightFace(@RequestBody InsightFace insightFace) {
        // 扣除接口费用并且保存消息记录
        chatCostService.taskDeduct("mj","换脸", NumberUtils.toDouble(priceConfig.getFaceSwapping(), 0.3));
        // 创建请求体（这里使用JSON作为媒体类型）
        String insightFaceJson = JSONUtil.toJsonStr(insightFace);
        String url = "mj/insight-face/swap";
        Request request = mjOkHttpUtil.createPostRequest(url, insightFaceJson);
        return mjOkHttpUtil.executeRequest(request);
    }

}
