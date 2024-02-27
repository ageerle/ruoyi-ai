package com.xmzs.midjourney.service;

import com.xmzs.midjourney.enums.BlendDimensions;
import com.xmzs.midjourney.result.SubmitResultVO;
import com.xmzs.midjourney.support.Task;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface TaskService {

	SubmitResultVO submitImagine(Task task, List<DataUrl> dataUrls);

	SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags);

	SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags);

	SubmitResultVO submitReroll(Task task, String targetMessageId, String targetMessageHash, int messageFlags);

	SubmitResultVO submitDescribe(Task task, DataUrl dataUrl);

	SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions);
}
