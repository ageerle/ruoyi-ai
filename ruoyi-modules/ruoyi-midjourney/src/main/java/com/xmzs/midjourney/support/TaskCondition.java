package com.xmzs.midjourney.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.xmzs.midjourney.Constants;
import com.xmzs.midjourney.enums.TaskAction;
import com.xmzs.midjourney.enums.TaskStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.function.Predicate;


@Data
@Accessors(chain = true)
public class TaskCondition implements Predicate<Task> {
	private String id;

	private Set<TaskStatus> statusSet;
	private Set<TaskAction> actionSet;

	private String prompt;
	private String promptEn;
	private String description;

	private String finalPromptEn;
	private String messageId;
	private String messageHash;
	private String progressMessageId;
	private String nonce;

	@Override
	public boolean test(Task task) {
		if (task == null) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.id) && !this.id.equals(task.getId())) {
			return false;
		}
		if (this.statusSet != null && !this.statusSet.isEmpty() && !this.statusSet.contains(task.getStatus())) {
			return false;
		}
		if (this.actionSet != null && !this.actionSet.isEmpty() && !this.actionSet.contains(task.getAction())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.prompt) && !this.prompt.equals(task.getPrompt())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.promptEn) && !this.promptEn.equals(task.getPromptEn())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.description) && !CharSequenceUtil.contains(task.getDescription(), this.description)) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.finalPromptEn) && !this.finalPromptEn.equals(task.getProperty(Constants.TASK_PROPERTY_FINAL_PROMPT))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.messageId) && !this.messageId.equals(task.getProperty(Constants.TASK_PROPERTY_MESSAGE_ID))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.messageHash) && !this.messageHash.equals(task.getProperty(Constants.TASK_PROPERTY_MESSAGE_HASH))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.progressMessageId) && !this.progressMessageId.equals(task.getProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.nonce) && !this.nonce.equals(task.getProperty(Constants.TASK_PROPERTY_NONCE))) {
			return false;
		}
		return true;
	}

}
