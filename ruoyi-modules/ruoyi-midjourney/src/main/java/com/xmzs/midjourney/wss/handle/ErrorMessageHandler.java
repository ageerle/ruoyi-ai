package com.xmzs.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.xmzs.midjourney.ProxyProperties;
import com.xmzs.midjourney.enums.MessageType;
import com.xmzs.midjourney.enums.TaskStatus;
import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.support.TaskCondition;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class ErrorMessageHandler extends MessageHandler {
	@Autowired
	protected ProxyProperties properties;

	@Override
	public void handle(MessageType messageType, DataObject message) {
		Optional<DataArray> embedsOptional = message.optArray("embeds");
		if (!MessageType.CREATE.equals(messageType) || embedsOptional.isEmpty() || embedsOptional.get().isEmpty()) {
			return;
		}
		DataObject embed = embedsOptional.get().getObject(0);
		String title = embed.getString("title", null);
		String description = embed.getString("description", null);
		String footerText = "";
		Optional<DataObject> footer = embed.optObject("footer");
		if (footer.isPresent()) {
			footerText = footer.get().getString("text", "");
		}
		String channelId = message.getString("channel_id", "");
		int color = embed.getInt("color", 0);
		if (color == 16239475) {
			log.warn("{} - MJ警告信息: {}\n{}\nfooter: {}", channelId, title, description, footerText);
		} else if (color == 16711680) {
			log.error("{} - MJ异常信息: {}\n{}\nfooter: {}", channelId, title, description, footerText);
			String nonce = getMessageNonce(message);
			Task task = this.discordLoadBalancer.getRunningTaskByNonce(nonce);
			if (task != null) {
				task.fail("[" + title + "] " + description);
				task.awake();
			}
		} else if (CharSequenceUtil.contains(title, "Invalid link")) {
			// 兼容 Invalid link! 错误
			log.error("{} - MJ异常信息: {}\n{}\nfooter: {}", channelId, title, description, footerText);
			DataObject messageReference = message.optObject("message_reference").orElse(DataObject.empty());
			String referenceMessageId = messageReference.getString("message_id", "");
			if (CharSequenceUtil.isBlank(referenceMessageId)) {
				return;
			}
			TaskCondition condition = new TaskCondition().setStatusSet(Set.of(TaskStatus.IN_PROGRESS))
					.setProgressMessageId(referenceMessageId);
			Task task = this.discordLoadBalancer.findRunningTask(condition).findFirst().orElse(null);
			if (task != null) {
				task.fail("[" + title + "] " + description);
				task.awake();
			}
		}
	}

}
