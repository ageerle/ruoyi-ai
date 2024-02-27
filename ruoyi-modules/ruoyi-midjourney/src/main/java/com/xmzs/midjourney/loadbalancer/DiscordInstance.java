package com.xmzs.midjourney.loadbalancer;


import com.xmzs.midjourney.domain.DiscordAccount;
import com.xmzs.midjourney.result.Message;
import com.xmzs.midjourney.result.SubmitResultVO;
import com.xmzs.midjourney.service.DiscordService;
import com.xmzs.midjourney.support.Task;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface DiscordInstance extends DiscordService {

	String getInstanceId();

	DiscordAccount account();

	boolean isAlive();

	void startWss() throws Exception;

	List<Task> getRunningTasks();

	void exitTask(Task task);

	Map<String, Future<?>> getRunningFutures();

	SubmitResultVO submitTask(Task task, Callable<Message<Void>> discordSubmit);

}
