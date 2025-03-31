package org.ruoyi.chat.controller;

import cn.hutool.json.JSONUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.ruoyi.chat.dto.TaskConditionDTO;
import org.ruoyi.chat.util.MjOkHttpUtil;
import org.springframework.web.bind.annotation.*;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/mj/task")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final MjOkHttpUtil mjOkHttpUtil;

	@ApiOperation(value = "指定ID获取任务")
	@GetMapping("/{id}/fetch")
    public String fetch(@ApiParam(value = "任务ID") @PathVariable String id) {
        String url = "mj/task/" + id + "/fetch";
        Request request = mjOkHttpUtil.createGetRequest(url);
        return mjOkHttpUtil.executeRequest(request);
    }

	@ApiOperation(value = "根据ID列表查询任务")
	@PostMapping("/list-by-condition")
	public String listByIds(@RequestBody TaskConditionDTO conditionDTO) {
        String url = "mj/task/list-by-condition";
        String conditionJson = JSONUtil.toJsonStr(conditionDTO);
        Request request = mjOkHttpUtil.createPostRequest(url,conditionJson);
        return mjOkHttpUtil.executeRequest(request);
	}

    @ApiOperation(value = "获取任务图片的seed")
    @GetMapping("/{id}/image-seed")
    public String getSeed(@ApiParam(value = "任务ID") @PathVariable String id) {
        String url = "mj/task/" + id + "/image-seed";
        Request request = mjOkHttpUtil.createGetRequest(url);
        return mjOkHttpUtil.executeRequest(request);
    }

}
