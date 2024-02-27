package com.xmzs.midjourney.wss.handle;


import com.xmzs.midjourney.enums.MessageType;
import com.xmzs.midjourney.enums.TaskAction;
import com.xmzs.midjourney.support.TaskCondition;
import com.xmzs.midjourney.util.ContentParseData;
import com.xmzs.midjourney.util.ConvertUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * imagine消息处理.
 * 完成(create): **cat** - <@1012983546824114217> (relaxed)
 */
@Component
public class ImagineSuccessHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		ContentParseData parseData = ConvertUtils.parseContent(content, CONTENT_REGEX);
		if (MessageType.CREATE.equals(messageType) && parseData != null && hasImage(message)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.IMAGINE))
					.setFinalPromptEn(parseData.getPrompt());
			findAndFinishImageTask(condition, parseData.getPrompt(), message);
		}
	}

}
