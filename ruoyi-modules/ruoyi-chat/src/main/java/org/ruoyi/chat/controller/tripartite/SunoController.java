package org.ruoyi.chat.controller.tripartite;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.chat.config.OkHttpConfig;
import org.ruoyi.chat.domain.bo.GenerateLyric;
import org.ruoyi.chat.domain.bo.GenerateSuno;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.core.utils.OkHttpUtil;
import org.springframework.web.bind.annotation.*;


/**
 * 文生歌曲任务提交
 *
 * @author ageerle
 * @date 2025-05-03
 */
@RestController
@RequestMapping("/sunoapi")
@RequiredArgsConstructor
@Slf4j
public class SunoController {

    private final OkHttpConfig okHttpConfig;
    private final IChatCostService chatCostService;

    @ApiOperation(value = "文生歌曲")
    @PostMapping("/generate")
    public String generate(@RequestBody GenerateSuno generateSuno) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("suno");
        // 扣除接口费用并且保存消息记录
        chatCostService.taskDeduct("suno","文生歌曲", NumberUtils.toDouble(okHttpConfig.getGenerate(), 0.3));
        // 创建请求体（这里使用JSON作为媒体类型）
        String generateJson = JSONUtil.toJsonStr(generateSuno);
        String url = "suno/generate";
        Request request = okHttpUtil.createPostRequest(url, generateJson);
        return okHttpUtil.executeRequest(request);
    }

    @ApiOperation(value = "生成歌词")
    @PostMapping("/generate/lyrics/")
    public String generate(@RequestBody GenerateLyric generateLyric) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("suno");
        String generateJson = JSONUtil.toJsonStr(generateLyric);
        String url = "task/suno/v1/submit/lyrics";
        Request request = okHttpUtil.createPostRequest(url, generateJson);
        return okHttpUtil.executeRequest(request);
    }


    @ApiOperation(value = "查询歌词任务")
    @GetMapping("/lyrics/{taskId}")
    public String lyrics(@PathVariable String taskId) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("suno");
        String url = "task/suno/v1/fetch/"+taskId;
        Request request = okHttpUtil.createGetRequest(url);
        return okHttpUtil.executeRequest(request);
    }


    @ApiOperation(value = "查询歌曲任务")
    @GetMapping("/feed/{taskId}")
    public String feed(@PathVariable String taskId) {
        OkHttpUtil okHttpUtil = okHttpConfig.getOkHttpUtil("suno");
        String url = "suno/feed/"+taskId;
        Request request = okHttpUtil.createGetRequest(url);
        return okHttpUtil.executeRequest(request);
    }

}
