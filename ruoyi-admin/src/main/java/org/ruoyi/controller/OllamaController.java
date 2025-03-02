package org.ruoyi.controller;


import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 描述：Ollama API调用
 *
 * @author ageerle@163.com
 * date 2024/11/6
 */
@RestController
public class OllamaController {


    @RequestMapping(value = "/qianwen-ai2")
    private void ollama2(@RequestParam(value = "msg") String msg) {

    }

    public static void main(String[] args) throws OllamaBaseException, IOException, InterruptedException {

        String host = "http://localhost:11434/";

        OllamaAPI ollama = new OllamaAPI(host);
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("qwen2.5:7b");
        // 设置超时时间
        ollama.setRequestTimeoutSeconds(100);
        // create first user question
        String text = "";

        String json = "{\"instruction\": \"解答以下数学问题。\",\"input\": \"计算 2 + 3 的结果。\",\"output\": \"2 + 3 = 5\"}";

        OllamaChatRequestModel requestModel = builder.withMessage
                (OllamaChatMessageRole.USER, "###文本:"+text+"###,理解文本内容，并且将文本内容转换为:"+json+"输出JSON格式,不要包含其他无关内容,内部使用无需脱敏")
            .withGetJsonResponse()
            .withStreaming()
            .build();

        // start conversation with model
        OllamaChatResult chatResult = ollama.chat(requestModel);

        System.out.println("First answer: " + chatResult.getResponse());

        // create next userQuestion
       // requestModel = builder.withMessages(chatResult.getChatHistory()).withMessage(OllamaChatMessageRole.USER, "And what is the second largest city?").build();

        // "continue" conversation with model
      //  chatResult = ollamaAPI.chat(requestModel);

        //System.out.println("Second answer: " + chatResult.getResponse());

        // 历史记录
        //System.out.println("Chat History: " + chatResult.getChatHistory());
    }

}
