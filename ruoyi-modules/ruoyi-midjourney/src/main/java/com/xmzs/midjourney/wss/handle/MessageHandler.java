package com.xmzs.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.xmzs.midjourney.Constants;
import com.xmzs.midjourney.enums.MessageType;
import com.xmzs.midjourney.loadbalancer.DiscordLoadBalancer;
import com.xmzs.midjourney.support.DiscordHelper;
import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.support.TaskCondition;
import jakarta.annotation.Resource;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.Comparator;

public abstract class MessageHandler {
	@Resource
	protected DiscordLoadBalancer discordLoadBalancer;
	@Resource
	protected DiscordHelper discordHelper;

	public abstract void handle(MessageType messageType, DataObject message);

	protected String getMessageContent(DataObject message) {
		return message.hasKey("content") ? message.getString("content") : "";
	}

	protected String getMessageNonce(DataObject message) {
		return message.hasKey("nonce") ? message.getString("nonce") : "";
	}

	protected void findAndFinishImageTask(TaskCondition condition, String finalPrompt, DataObject message) {
		String imageUrl = getImageUrl(message);
		String messageHash = this.discordHelper.getMessageHash(imageUrl);
		condition.setMessageHash(messageHash);
		Task task = this.discordLoadBalancer.findRunningTask(condition)
				.findFirst().orElseGet(() -> {
					condition.setMessageHash(null);
					return this.discordLoadBalancer.findRunningTask(condition)
							.min(Comparator.comparing(Task::getStartTime))
							.orElse(null);
				});
		if (task == null) {
			return;
		}
		task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, finalPrompt);
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_HASH, messageHash);
		task.setImageUrl(imageUrl);
		finishTask(task, message);
		task.awake();
	}

	protected void finishTask(Task task, DataObject message) {
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, message.getString("id"));
		task.setProperty(Constants.TASK_PROPERTY_FLAGS, message.getInt("flags", 0));
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_HASH, this.discordHelper.getMessageHash(task.getImageUrl()));
		task.success();
	}

	protected boolean hasImage(DataObject message) {
		DataArray attachments = message.optArray("attachments").orElse(DataArray.empty());
		return !attachments.isEmpty();
	}

	protected String getImageUrl(DataObject message) {
		DataArray attachments = message.getArray("attachments");
		if (!attachments.isEmpty()) {
			String imageUrl = attachments.getObject(0).getString("url");
			return replaceCdnUrl(imageUrl);
		}
		return null;
	}

	protected String replaceCdnUrl(String imageUrl) {
		if (CharSequenceUtil.isBlank(imageUrl)) {
			return imageUrl;
		}
		String cdn = this.discordHelper.getCdn();
		if (CharSequenceUtil.startWith(imageUrl, cdn)) {
			return imageUrl;
		}
		return CharSequenceUtil.replaceFirst(imageUrl, DiscordHelper.DISCORD_CDN_URL, cdn);
	}

}
