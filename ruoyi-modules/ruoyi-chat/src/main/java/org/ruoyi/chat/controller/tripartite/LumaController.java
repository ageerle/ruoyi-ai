package org.ruoyi.chat.controller.tripartite;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.chat.config.OkHttpConfig;
import org.ruoyi.chat.domain.bo.GenerateLuma;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.core.utils.OkHttpUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 文生视频
 *
 * @author ageerle
 * @date 2025-05-03
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
