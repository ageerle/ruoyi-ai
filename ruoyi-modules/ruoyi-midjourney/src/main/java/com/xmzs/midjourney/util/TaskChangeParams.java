package com.xmzs.midjourney.util;

import com.xmzs.midjourney.enums.TaskAction;
import lombok.Data;

@Data
public class TaskChangeParams {
	private String id;
	private TaskAction action;
	private Integer index;
}
