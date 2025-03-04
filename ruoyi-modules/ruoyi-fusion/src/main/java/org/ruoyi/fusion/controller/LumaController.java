package org.ruoyi.fusion.controller;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.utils.OkHttpUtil;
import org.ruoyi.system.cofing.OkHttpConfig;
import org.ruoyi.system.domain.GenerateLuma;
import org.ruoyi.system.service.IChatCostService;
import org.springframework.web.bind.annotation.*;

/**
 * 描述：文生视频
 *
 * @author ageerle@163.com
 * date 2024/6/27
 */
@RestController
@RequestMapping("/luma")
@RequiredArgsConstructor
@Slf4j
public class LumaController {

    private final OkHttpConfig okHttpConfig;
    private final IChatCostService chatCostService;


    @ApiOperation(value = "文生视频")
    @PostMapping("/generations/")
    public String generateVideo(@RequestBody GenerateLuma generateLuma) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("luma");

        chatCostService.taskDeduct("luma", "文生视频", NumberUtils.toDouble(okHttpConfig.getGenerate(), 0.3));
        String generateJson = JSONUtil.toJsonStr(generateLuma);
        String url = "luma/generations";
        Request request = okHttpUtil.createPostRequest(url, generateJson);
        return okHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "文生视频任务查询")
    @GetMapping("/generations/{taskId}")
    public String getGenerationTask(@PathVariable String taskId) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("luma");
        String url = "luma/generations/" + taskId;
        Request request = okHttpUtil.createGetRequest(url);
        return okHttpUtil.executeRequest(request);
    }

}
