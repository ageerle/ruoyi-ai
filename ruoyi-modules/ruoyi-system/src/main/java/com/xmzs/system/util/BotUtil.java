//package com.xmzs.system.util;
//
//
//import com.xmzs.system.cofing.GptConfig;
//import com.xmzs.system.domain.ChatMessage;
//import jakarta.annotation.PostConstruct;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//
///**
// * bot工具类
// *
// * @author ashinnotfound
// * @date 2023/2/1
// */
//@Component
//@RequiredArgsConstructor
//public class BotUtil {
//
//    private final GptConfig gptConfig;
//
//    private GptClient gptClient;
//
//    private Tokenizer tokenizer;
//
//    private final Map<String, List<ChatMessage>> PROMPT_MAP = new HashMap<>();
//    private final Map<OpenAiService, Integer> COUNT_FOR_OPEN_AI_SERVICE = new HashMap<>();
//    @Getter
//    private ChatCompletionRequest.ChatCompletionRequestBuilder completionRequestBuilder;
//    private final List<ChatMessage> BASIC_PROMPT_LIST = new ArrayList<>();
//
//    @PostConstruct
//    public void init() {
//        completionRequestBuilder = ChatCompletionRequest.builder().model(gptConfig.getModel()).temperature(gptConfig.getTemperature()).maxTokens(gptConfig.getMaxToken());
//        for (OpenAiService openAiService : gptClient.getOpenAiServiceList()) {
//            COUNT_FOR_OPEN_AI_SERVICE.put(openAiService, 0);
//        }
//        for (String prompt : gptConfig.getBasicPrompt()){
//            BASIC_PROMPT_LIST.add(new ChatMessage("system", prompt));
//        }
//    }
//
//    public OpenAiService getOpenAiService() {
//        //获取使用次数最小的openAiService 否则获取map中的第一个
//        Optional<OpenAiService> openAiServiceToUse = COUNT_FOR_OPEN_AI_SERVICE.entrySet().stream()
//                .min(Map.Entry.comparingByValue())
//                .map(Map.Entry::getKey);
//        if (openAiServiceToUse.isPresent()) {
//            COUNT_FOR_OPEN_AI_SERVICE.put(openAiServiceToUse.get(), COUNT_FOR_OPEN_AI_SERVICE.get(openAiServiceToUse.get()) + 1);
//            return openAiServiceToUse.get();
//        } else {
//            COUNT_FOR_OPEN_AI_SERVICE.put(COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next(), COUNT_FOR_OPEN_AI_SERVICE.get(COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next()) + 1);
//            return COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next();
//        }
//    }
//
//    public List<ChatMessage> buildPrompt(String sessionId, String newPrompt) {
//        if (!PROMPT_MAP.containsKey(sessionId)) {
//            if (!BASIC_PROMPT_LIST.isEmpty()){
//                List<ChatMessage> promptList = new ArrayList<>(BASIC_PROMPT_LIST);
//                PROMPT_MAP.put(sessionId, promptList);
//            }
//        }
//        List<ChatMessage> promptList = PROMPT_MAP.getOrDefault(sessionId, new ArrayList<>());
//        promptList.add(new ChatMessage("user", newPrompt));
//        if (tokenizer.countMessageTokens(gptConfig.getModel(), promptList) > gptConfig.getMaxToken()){
//            List<ChatMessage> tempChatMessage = deleteFirstPrompt(sessionId);
//            if (tempChatMessage != null){
//                return buildPrompt(sessionId, newPrompt);
//            }
//            return null;
//        }
//        return promptList;
//    }
//
//    public boolean isPromptEmpty(String sessionId){
//        if (!PROMPT_MAP.containsKey(sessionId)){
//            return true;
//        }
//        return PROMPT_MAP.get(sessionId).size() == BASIC_PROMPT_LIST.size();
//    }
//    public List<ChatMessage> deleteFirstPrompt(String sessionId) {
//        if (!isPromptEmpty(sessionId)){
//            int index = BASIC_PROMPT_LIST.size();
//            List<ChatMessage> promptList = PROMPT_MAP.get(sessionId);
//            //问
//            promptList.remove(index);
//            //答
//            if (index < promptList.size()){
//                promptList.remove(index);
//                return promptList;
//            }else {
//                // 已经是初始聊天记录
//                return null;
//            }
//        }
//        // 已经是初始聊天记录
//        return null;
//    }
//
//    public void resetPrompt(String sessionId) {
//        PROMPT_MAP.remove(sessionId);
//    }
//}
