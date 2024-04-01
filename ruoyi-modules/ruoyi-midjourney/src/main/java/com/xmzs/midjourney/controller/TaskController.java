package com.xmzs.midjourney.controller;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.json.JSONUtil;
import com.xmzs.midjourney.dto.SubmitImagineDTO;
import com.xmzs.midjourney.dto.TaskConditionDTO;
import com.xmzs.midjourney.loadbalancer.DiscordLoadBalancer;
import com.xmzs.midjourney.result.SubmitResultVO;
import com.xmzs.midjourney.service.TaskStoreService;
import com.xmzs.midjourney.support.Task;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/mj/task")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
	private final TaskStoreService taskStoreService;
	private final DiscordLoadBalancer discordLoadBalancer;

    @Value("${chat.apiKey}")
    private String apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;

	@ApiOperation(value = "指定ID获取任务")
	@GetMapping("/{id}/fetch")
    public String fetch(@ApiParam(value = "任务ID") @PathVariable String id) {
        OkHttpClient client = new OkHttpClient();
        // 创建一个Request对象来配置你的请求
        Request request = new Request.Builder()
            .header("mj-api-secret", apiKey) // 设置Authorization header
            .url(apiHost+"mj/task/" + id + "/fetch")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            log.error("任务:{}查询失败:{}",id,e.getMessage());
        }
        return null;
    }

	@ApiOperation(value = "查询任务队列")
	@GetMapping("/queue")
	public List<Task> queue() {
		return this.discordLoadBalancer.getQueueTaskIds().stream()
				.map(this.taskStoreService::get).filter(Objects::nonNull)
				.sorted(Comparator.comparing(Task::getSubmitTime))
				.toList();
	}

	@ApiOperation(value = "查询所有任务")
	@GetMapping("/list")
	public List<Task> list() {
		return this.taskStoreService.list().stream()
				.sorted((t1, t2) -> CompareUtil.compare(t2.getSubmitTime(), t1.getSubmitTime()))
				.toList();
	}

	@ApiOperation(value = "根据ID列表查询任务")
	@PostMapping("/list-by-condition")
	public List<Task> listByIds(@RequestBody TaskConditionDTO conditionDTO) {
		if (conditionDTO.getIds() == null) {
			return Collections.emptyList();
		}
		return conditionDTO.getIds().stream().map(this.taskStoreService::get).filter(Objects::nonNull).toList();
	}

}
