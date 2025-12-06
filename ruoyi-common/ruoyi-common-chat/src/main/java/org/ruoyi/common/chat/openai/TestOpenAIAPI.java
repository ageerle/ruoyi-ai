package org.ruoyi.common.chat.openai;

import okhttp3.*;

import java.io.IOException;

public class TestOpenAIAPI {

    private static final String API_KEY = "sk-Waea254YSRYVg4FZVCz2CDz73B22xRpmKpJ41kbczVgpPxvg";
    private static final String URL = "https://api.gptgod.online/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) throws IOException {
        TestOpenAIAPI api = new TestOpenAIAPI();
        api.getChatGptResponse("Hello, how are you?");
    }

    public void getChatGptResponse(String prompt) throws IOException {
        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, {\"role\": \"user\", \"content\": \"" + prompt + "\"}]}");

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {

        }
    }
}
