package com.xmzs.midjourney.wss.handle;


import com.xmzs.midjourney.enums.MessageType;
import com.xmzs.midjourney.enums.TaskAction;
import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.support.TaskCondition;
import com.xmzs.midjourney.util.ContentParseData;
import com.xmzs.midjourney.util.ConvertUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * blend消息处理.
 * 完成(create): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (relaxed)
 */
@Component
public class BlendSuccessHandler extends MessageHandler {

	@Override
	public void handle(MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		ContentParseData parseData = ConvertUtils.parseContent(content);
		if (parseData == null || !MessageType.CREATE.equals(messageType)) {
			return;
		}
		Optional<DataObject> interaction = message.optObject("interaction");
		if (interaction.isPresent() && "blend".equals(interaction.get().getString("name"))) {
			// blend任务开始时，设置prompt
			Task task = this.discordLoadBalancer.getRunningTaskByNonce(getMessageNonce(message));
			if (task != null) {
				task.setPromptEn(parseData.getPrompt());
				task.setPrompt(parseData.getPrompt());
			}
		}
		if (hasImage(message)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.BLEND))
					.setFinalPromptEn(parseData.getPrompt());
			findAndFinishImageTask(condition, parseData.getPrompt(), message);
		}
	}

}
