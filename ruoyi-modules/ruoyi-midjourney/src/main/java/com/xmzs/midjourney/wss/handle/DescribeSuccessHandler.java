package com.xmzs.midjourney.wss.handle;

import com.xmzs.midjourney.Constants;
import com.xmzs.midjourney.enums.MessageType;
import com.xmzs.midjourney.support.Task;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * describe消息处理.
 */
@Component
public class DescribeSuccessHandler extends MessageHandler {

	@Override
	public void handle(MessageType messageType, DataObject message) {
		Optional<DataObject> interaction = message.optObject("interaction");
		if (!MessageType.UPDATE.equals(messageType) || interaction.isEmpty() || !"describe".equals(interaction.get().getString("name"))) {
			return;
		}
		DataArray embeds = message.getArray("embeds");
		if (embeds.isEmpty()) {
			return;
		}
		String description = embeds.getObject(0).getString("description");
		Optional<DataObject> imageOptional = embeds.getObject(0).optObject("image");
		if (imageOptional.isEmpty()) {
			return;
		}
		String imageUrl = imageOptional.get().getString("url");
		String taskId = this.discordHelper.findTaskIdWithCdnUrl(imageUrl);
		Task task = this.discordLoadBalancer.getRunningTask(taskId);
		if (task == null) {
			return;
		}
		task.setPrompt(description);
		task.setPromptEn(description);
		task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, description);
		task.setImageUrl(replaceCdnUrl(imageUrl));
		finishTask(task, message);
		task.awake();
	}

}
