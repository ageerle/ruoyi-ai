package org.ruoyi.common.chat.demo.zhipu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.deserialize.MessageDeserializeFactory;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AllToolsTest {

    private final static Logger logger = LoggerFactory.getLogger(AllToolsTest.class);
    private static final String API_SECRET_KEY = "28550a39d4cfaabbbf38df04dd3931f5.IUvfTThUf0xBF5l0";

    private static final ClientV4 client = new ClientV4.Builder(API_SECRET_KEY)
            .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
            .build();
    private static final ObjectMapper mapper = MessageDeserializeFactory.defaultObjectMapper();
    // 请自定义自己的业务id
    private static final String requestIdTemplate = "mycompany-%d";


    @Test
    public void test1() throws JsonProcessingException {


        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "帮我查询北京天气");
        messages.add(chatMessage);
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        // 函数调用参数构建部分
        List<ChatTool> chatToolList = new ArrayList<>();
        ChatTool chatTool = new ChatTool();

        chatTool.setType("code_interpreter");
        ObjectNode objectNode =  mapper.createObjectNode();
        objectNode.put("code", "北京天气");
//        chatTool.set(chatFunction);
        chatToolList.add(chatTool);


        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("glm-4-alltools")
                .stream(Boolean.TRUE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .tools(chatToolList)
                .toolChoice("auto")
                .requestId(requestId)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        if (sseModelApiResp.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            List<Choice> choices = new ArrayList<>();
            AtomicReference<ChatMessageAccumulator> lastAccumulator = new AtomicReference<>();

            mapStreamToAccumulator(sseModelApiResp.getFlowable())
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
                            lastAccumulator.set(accumulator);

                        }
                    })
                    .doOnComplete(() -> System.out.println("Stream completed."))
                    .doOnError(throwable -> System.err.println("Error: " + throwable)) // Handle errors
                    .blockingSubscribe();// Use blockingSubscribe instead of blockingGet()

            ChatMessageAccumulator chatMessageAccumulator = lastAccumulator.get();
            ModelData data = new ModelData();
            data.setChoices(choices);
            if (chatMessageAccumulator != null) {
                data.setUsage(chatMessageAccumulator.getUsage());
                data.setId(chatMessageAccumulator.getId());
                data.setCreated(chatMessageAccumulator.getCreated());
            }
            data.setRequestId(chatCompletionRequest.getRequestId());
            sseModelApiResp.setFlowable(null);// 打印前置空
            sseModelApiResp.setData(data);
        }
        logger.info("model output: {}", mapper.writeValueAsString(sseModelApiResp));
        client.getConfig().getHttpClient().dispatcher().executorService().shutdown();

        client.getConfig().getHttpClient().connectionPool().evictAll();
        // List all active threads
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            logger.info("Thread: " + t.getName() + " State: " + t.getState());
        }
    }



    public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> {
            return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
        });
    }
}
