package com.xmzs.midjourney.loadbalancer;


import com.xmzs.midjourney.Constants;
import com.xmzs.midjourney.ReturnCode;
import com.xmzs.midjourney.domain.DiscordAccount;
import com.xmzs.midjourney.enums.BlendDimensions;
import com.xmzs.midjourney.enums.TaskStatus;
import com.xmzs.midjourney.result.Message;
import com.xmzs.midjourney.result.SubmitResultVO;
import com.xmzs.midjourney.service.DiscordService;
import com.xmzs.midjourney.service.DiscordServiceImpl;
import com.xmzs.midjourney.service.NotifyService;
import com.xmzs.midjourney.service.TaskStoreService;
import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.wss.WebSocketStarter;
import com.xmzs.midjourney.wss.user.UserWebSocketStarter;
import eu.maxschuster.dataurl.DataUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
public class DiscordInstanceImpl implements DiscordInstance {
	private final DiscordAccount account;
	private final WebSocketStarter socketStarter;
	private final DiscordService service;
	private final TaskStoreService taskStoreService;
	private final NotifyService notifyService;

	private final ThreadPoolTaskExecutor taskExecutor;
	private final List<Task> runningTasks;
	private final Map<String, Future<?>> taskFutureMap = Collections.synchronizedMap(new HashMap<>());

	public DiscordInstanceImpl(DiscordAccount account, UserWebSocketStarter socketStarter, RestTemplate restTemplate,
			TaskStoreService taskStoreService, NotifyService notifyService, Map<String, String> paramsMap) {
		this.account = account;
		this.socketStarter = socketStarter;
		this.taskStoreService = taskStoreService;
		this.notifyService = notifyService;
		this.service = new DiscordServiceImpl(account, restTemplate, paramsMap);
		this.runningTasks = new CopyOnWriteArrayList<>();
		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(account.getCoreSize());
		this.taskExecutor.setMaxPoolSize(account.getCoreSize());
		this.taskExecutor.setQueueCapacity(account.getQueueSize());
		this.taskExecutor.setThreadNamePrefix("TaskQueue-" + account.getDisplay() + "-");
		this.taskExecutor.initialize();
	}

	@Override
	public String getInstanceId() {
		return this.account.getChannelId();
	}

	@Override
	public DiscordAccount account() {
		return this.account;
	}

	@Override
	public boolean isAlive() {
		return this.account.isEnable();
	}

	@Override
	public void startWss() throws Exception {
		this.socketStarter.setTrying(true);
		this.socketStarter.start();
	}

	@Override
	public List<Task> getRunningTasks() {
		return this.runningTasks;
	}

	@Override
	public void exitTask(Task task) {
		try {
			Future<?> future = this.taskFutureMap.get(task.getId());
			if (future != null) {
				future.cancel(true);
			}
			saveAndNotify(task);
		} finally {
			this.runningTasks.remove(task);
			this.taskFutureMap.remove(task.getId());
		}
	}

	@Override
	public Map<String, Future<?>> getRunningFutures() {
		return this.taskFutureMap;
	}

	@Override
	public synchronized SubmitResultVO submitTask(Task task, Callable<Message<Void>> discordSubmit) {
		this.taskStoreService.save(task);
		int currentWaitNumbers;
		try {
			currentWaitNumbers = this.taskExecutor.getThreadPoolExecutor().getQueue().size();
			Future<?> future = this.taskExecutor.submit(() -> executeTask(task, discordSubmit));
			this.taskFutureMap.put(task.getId(), future);
		} catch (RejectedExecutionException e) {
			this.taskStoreService.delete(task.getId());
			return SubmitResultVO.fail(ReturnCode.QUEUE_REJECTED, "队列已满，请稍后尝试")
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		} catch (Exception e) {
			log.error("submit task error", e);
			return SubmitResultVO.fail(ReturnCode.FAILURE, "提交失败，系统异常")
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		}
		if (currentWaitNumbers == 0) {
			return SubmitResultVO.of(ReturnCode.SUCCESS, "提交成功", task.getId())
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		} else {
			return SubmitResultVO.of(ReturnCode.IN_QUEUE, "排队中，前面还有" + currentWaitNumbers + "个任务", task.getId())
					.setProperty("numberOfQueues", currentWaitNumbers)
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		}
	}

	private void executeTask(Task task, Callable<Message<Void>> discordSubmit) {
		this.runningTasks.add(task);
		try {
			task.start();
			Message<Void> result = discordSubmit.call();
			if (result.getCode() != ReturnCode.SUCCESS) {
				task.fail(result.getDescription());
				saveAndNotify(task);
				return;
			}
			saveAndNotify(task);
			do {
				task.sleep();
				saveAndNotify(task);
			} while (task.getStatus() == TaskStatus.IN_PROGRESS);
			log.debug("task finished, id: {}, status: {}", task.getId(), task.getStatus());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("task execute error", e);
			task.fail("执行错误，系统异常");
			saveAndNotify(task);
		} finally {
			this.runningTasks.remove(task);
			this.taskFutureMap.remove(task.getId());
		}
	}

	private void saveAndNotify(Task task) {
		this.taskStoreService.save(task);
		this.notifyService.notifyTaskChange(task);
	}

	@Override
	public Message<Void> imagine(String prompt, String nonce) {
		return this.service.imagine(prompt, nonce);
	}

	@Override
	public Message<Void> upscale(String messageId, int index, String messageHash, int messageFlags, String nonce) {
		return this.service.upscale(messageId, index, messageHash, messageFlags, nonce);
	}

	@Override
	public Message<Void> variation(String messageId, int index, String messageHash, int messageFlags, String nonce) {
		return this.service.variation(messageId, index, messageHash, messageFlags, nonce);
	}

	@Override
	public Message<Void> reroll(String messageId, String messageHash, int messageFlags, String nonce) {
		return this.service.reroll(messageId, messageHash, messageFlags, nonce);
	}

	@Override
	public Message<Void> describe(String finalFileName, String nonce) {
		return this.service.describe(finalFileName, nonce);
	}

	@Override
	public Message<Void> blend(List<String> finalFileNames, BlendDimensions dimensions, String nonce) {
		return this.service.blend(finalFileNames, dimensions, nonce);
	}

	@Override
	public Message<String> upload(String fileName, DataUrl dataUrl) {
		return this.service.upload(fileName, dataUrl);
	}

	@Override
	public Message<String> sendImageMessage(String content, String finalFileName) {
		return this.service.sendImageMessage(content, finalFileName);
	}

}
