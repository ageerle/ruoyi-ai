package org.ruoyi.common.chat.demo.zhipu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.tools.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.core.response.HttpxBinaryResponseContent;
import com.zhipu.oapi.service.v4.batchs.BatchCreateParams;
import com.zhipu.oapi.service.v4.batchs.BatchResponse;
import com.zhipu.oapi.service.v4.batchs.QueryBatchResponse;
import com.zhipu.oapi.service.v4.embedding.EmbeddingApiResponse;
import com.zhipu.oapi.service.v4.embedding.EmbeddingRequest;
import com.zhipu.oapi.service.v4.file.*;
import com.zhipu.oapi.service.v4.fine_turning.*;
import com.zhipu.oapi.service.v4.image.CreateImageRequest;
import com.zhipu.oapi.service.v4.image.ImageApiResponse;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class WebSearchToolsTest {

    private final static Logger logger = LoggerFactory.getLogger(WebSearchToolsTest.class);
    private static final String API_SECRET_KEY = "xx";

    private static final ClientV4 client = new ClientV4.Builder(API_SECRET_KEY)
            .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();
    // 请自定义自己的业务id
    private static final String requestIdTemplate = "mycompany-%d";


    @Test
    public void test1() throws JsonProcessingException {

//        json 转换  ArrayList<SearchChatMessage>
        String jsonString = "[\n" +
                "                {\n" +
                "                    \"content\": \"今天武汉天气怎么样\",\n" +
                "                    \"role\": \"user\"\n" +
                "                }\n" +
                "            ]";

        ArrayList<SearchChatMessage> messages = new ObjectMapper().readValue(jsonString, new TypeReference<ArrayList<SearchChatMessage>>() {
        });


        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        WebSearchParamsRequest chatCompletionRequest = WebSearchParamsRequest.builder()
                .model("web-search-pro")
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId)
                .build();
        WebSearchApiResponse webSearchApiResponse = client.webSearchProStreamingInvoke(chatCompletionRequest);
        if (webSearchApiResponse.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            List<ChoiceDelta> choices = new ArrayList<>();
            AtomicReference<WebSearchPro> lastAccumulator = new AtomicReference<>();

            webSearchApiResponse.getFlowable().map(result -> result)
                    .doOnNext(accumulator -> {
                        {
                            if (isFirst.getAndSet(false)) {
                                logger.info("Response: ");
                            }
                            ChoiceDelta delta = accumulator.getChoices().get(0).getDelta();
                            if (delta != null && delta.getToolCalls() != null) {
                                logger.info("tool_calls: {}", mapper.writeValueAsString(delta.getToolCalls()));
                            }
                            choices.add(delta);
                            lastAccumulator.set(accumulator);

                        }
                    })
                    .doOnComplete(() -> System.out.println("Stream completed."))
                    .doOnError(throwable -> System.err.println("Error: " + throwable)) // Handle errors
                    .blockingSubscribe();// Use blockingSubscribe instead of blockingGet()

            WebSearchPro chatMessageAccumulator = lastAccumulator.get();

            webSearchApiResponse.setFlowable(null);// 打印前置空
            webSearchApiResponse.setData(chatMessageAccumulator);
        }
        logger.info("model output: {}", mapper.writeValueAsString(webSearchApiResponse));
        client.getConfig().getHttpClient().dispatcher().executorService().shutdown();

        client.getConfig().getHttpClient().connectionPool().evictAll();
        // List all active threads
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            logger.info("Thread: " + t.getName() + " State: " + t.getState());
        }

    }


    @Test
    public void test2() throws JsonProcessingException {

//        json 转换  ArrayList<SearchChatMessage>
        String jsonString = "[\n" +
                "                {\n" +
                "                    \"content\": \"今天天气怎么样\",\n" +
                "                    \"role\": \"user\"\n" +
                "                }\n" +
                "            ]";

        ArrayList<SearchChatMessage> messages = new ObjectMapper().readValue(jsonString, new TypeReference<ArrayList<SearchChatMessage>>() {
        });


        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        WebSearchParamsRequest chatCompletionRequest = WebSearchParamsRequest.builder()
                .model("web-search-pro")
                .stream(Boolean.FALSE)
                .messages(messages)
                .requestId(requestId)
                .build();
        WebSearchApiResponse webSearchApiResponse = client.invokeWebSearchPro(chatCompletionRequest);

        logger.info("model output: {}", mapper.writeValueAsString(webSearchApiResponse));

    }


    @Test
    public void testFunctionSSE() throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "成都到北京要多久，天气如何");
        messages.add(chatMessage);
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        // 函数调用参数构建部分
        List<ChatTool> chatToolList = new ArrayList<>();
        ChatTool chatTool = new ChatTool();

        chatTool.setType(ChatToolType.FUNCTION.value());
        ChatFunctionParameters chatFunctionParameters = new ChatFunctionParameters();
        chatFunctionParameters.setType("object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("location", new HashMap<String, Object>() {{
            put("type", "string");
            put("description", "城市，如：北京");
        }});
        properties.put("unit", new HashMap<String, Object>() {{
            put("type", "string");
            put("enum", new ArrayList<String>() {{
                add("celsius");
                add("fahrenheit");
            }});
        }});
        chatFunctionParameters.setProperties(properties);
        ChatFunction chatFunction = ChatFunction.builder()
                .name("get_weather")
                .description("Get the current weather of a location")
                .parameters(chatFunctionParameters)
                .build();
        chatTool.setFunction(chatFunction);
        chatToolList.add(chatTool);
        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);
        extraJson.put("max_tokens", 50);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId)
                .tools(chatToolList)
                .toolChoice("auto")
                .extraJson(extraJson)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        if (sseModelApiResp.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            List<Choice> choices = new ArrayList<>();
            ChatMessageAccumulator chatMessageAccumulator = mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        {
                            if (isFirst.getAndSet(false)) {
                                logger.info("Response: ");
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getTool_calls() != null) {
                                String jsonString = mapper.writeValueAsString(accumulator.getDelta().getTool_calls());
                                logger.info("tool_calls: {}", jsonString);
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                                logger.info(accumulator.getDelta().getContent());
                            }
                            choices.add(accumulator.getChoice());
                        }
                    })
                    .doOnComplete(System.out::println)
                    .lastElement()
                    .blockingGet();


            ModelData data = new ModelData();
            data.setChoices(choices);
            data.setUsage(chatMessageAccumulator.getUsage());
            data.setId(chatMessageAccumulator.getId());
            data.setCreated(chatMessageAccumulator.getCreated());
            data.setRequestId(chatCompletionRequest.getRequestId());
            sseModelApiResp.setFlowable(null);// 打印前置空
            sseModelApiResp.setData(data);
        }
        logger.info("model output: {}", mapper.writeValueAsString(sseModelApiResp));
    }

    public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> {
            return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
        });
    }

}
