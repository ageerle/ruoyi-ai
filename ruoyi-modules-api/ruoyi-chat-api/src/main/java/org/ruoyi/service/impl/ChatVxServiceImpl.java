package org.ruoyi.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.service.IChatVxService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatVxServiceImpl implements IChatVxService {

    private final OpenAiStreamClient openAiStreamClient;

    @Override
    public String chat(String prompt) {
        List<Message> messageList = new ArrayList<>();
        Message message = Message.builder().role(Message.Role.USER).content(prompt).build();
        messageList.add(message);
        ChatCompletion chatCompletion = ChatCompletion
            .builder()
            .messages(messageList)
            .model("gpt-4o-mini")
            .stream(false)
            .build();
        ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
        return chatCompletionResponse.getChoices().get(0).getMessage().getContent().toString();
    }

}
