package com.xmzs.midjourney.service;


import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.support.TaskCondition;

import java.util.List;

public interface TaskStoreService {

	void save(Task task);

	void delete(String id);

	Task get(String id);

	List<Task> list();

	List<Task> list(TaskCondition condition);

	Task findOne(TaskCondition condition);

}
