package org.ruoyi.chat.controller.tripartite;

import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.ruoyi.chat.domain.dto.TaskConditionDTO;
import org.ruoyi.chat.util.MjOkHttpUtil;
import org.springframework.web.bind.annotation.*;


/**
 * 绘画任务查询
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Tag(name = "任务查询")
@RestController
@RequestMapping("/mj/task")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final MjOkHttpUtil mjOkHttpUtil;

    @Operation(summary = "指定ID获取任务")
    @GetMapping("/{id}/fetch")
    public String fetch(@Parameter(description = "任务ID") @PathVariable String id) {
        String url = "mj/task/" + id + "/fetch";
        Request request = mjOkHttpUtil.createGetRequest(url);
        return mjOkHttpUtil.executeRequest(request);
    }

    @Operation(summary = "根据ID列表查询任务")
    @PostMapping("/list-by-condition")
    public String listByIds(@RequestBody TaskConditionDTO conditionDTO) {
        String url = "mj/task/list-by-condition";
        String conditionJson = JSONUtil.toJsonStr(conditionDTO);
        Request request = mjOkHttpUtil.createPostRequest(url, conditionJson);
        return mjOkHttpUtil.executeRequest(request);
    }

    @Operation(summary = "获取任务图片的seed")
    @GetMapping("/{id}/image-seed")
    public String getSeed(@Parameter(description = "任务ID") @PathVariable String id) {
        String url = "mj/task/" + id + "/image-seed";
        Request request = mjOkHttpUtil.createGetRequest(url);
        return mjOkHttpUtil.executeRequest(request);
    }

}
