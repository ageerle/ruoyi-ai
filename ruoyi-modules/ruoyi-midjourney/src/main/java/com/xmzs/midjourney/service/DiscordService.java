package com.xmzs.midjourney.service;


import com.xmzs.midjourney.enums.BlendDimensions;
import com.xmzs.midjourney.result.Message;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface DiscordService {

	Message<Void> imagine(String prompt, String nonce);

	Message<Void> upscale(String messageId, int index, String messageHash, int messageFlags, String nonce);

	Message<Void> variation(String messageId, int index, String messageHash, int messageFlags, String nonce);

	Message<Void> reroll(String messageId, String messageHash, int messageFlags, String nonce);

	Message<Void> describe(String finalFileName, String nonce);

	Message<Void> blend(List<String> finalFileNames, BlendDimensions dimensions, String nonce);

	Message<String> upload(String fileName, DataUrl dataUrl);

	Message<String> sendImageMessage(String content, String finalFileName);

}
