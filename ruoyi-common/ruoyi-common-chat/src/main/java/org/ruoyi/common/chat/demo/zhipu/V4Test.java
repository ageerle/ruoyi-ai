package org.ruoyi.common.chat.demo.zhipu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class V4Test {

    private final static Logger logger = LoggerFactory.getLogger(V4Test.class);
    private static final String API_SECRET_KEY = "28550a39d4cfaabbbf38df04dd3931f5.IUvfTThUf0xBF5l0";


    private static final ClientV4 client = new ClientV4.Builder(API_SECRET_KEY)
            .enableTokenCache()
            .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
            .build();

    // 请自定义自己的业务id
    private static final String requestIdTemplate = "mycompany-%d";

    private static final ObjectMapper mapper = new ObjectMapper();


    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    @Test
    public void test() {

    }

    /**
     * sse-V4：function调用
     */
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


    /**
     * sse-V4：非function调用
     */
    @Test
    public void testNonFunctionSSE() throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "ChatGLM和你哪个更强大");
        messages.add(chatMessage);
        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);
        extraJson.put("max_tokens", 3);

        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId)
                .extraJson(extraJson)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        // stream 处理方法
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
                                logger.info("accumulator.getDelta().getContent(): {}", accumulator.getDelta().getContent());
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


    /**
     * V4-同步function调用
     */
    @Test
    public void testFunctionInvoke() {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "你可以做什么");
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


        ChatTool chatTool1 = new ChatTool();
        chatTool1.setType(ChatToolType.WEB_SEARCH.value());
        WebSearch webSearch = new WebSearch();
        webSearch.setSearch_query("清华的升学率");
        webSearch.setSearch_result(true);
        webSearch.setEnable(false);
        chatTool1.setWeb_search(webSearch);

        chatToolList.add(chatTool);
        chatToolList.add(chatTool1);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .tools(chatToolList)
                .toolChoice("auto")
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        try {
            logger.info("model output: {}", mapper.writeValueAsString(invokeModelApiResp));
        } catch (JsonProcessingException e) {
            logger.error("model output error", e);
        }
    }


    /**
     * V4-同步非function调用
     */
    @Test
    public void testNonFunctionInvoke() throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "ChatGLM和你哪个更强大");
        messages.add(chatMessage);
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());


        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);
        extraJson.put("max_tokens", 3);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .extraJson(extraJson)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        logger.info("model output: {}", mapper.writeValueAsString(invokeModelApiResp));
    }


    /**
     * V4-同步非function调用
     */
    @Test
    public void testCharGlmInvoke() throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "ChatGLM和你哪个更强大");
        messages.add(chatMessage);
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());


        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);

        ChatMeta meta = new ChatMeta();
        meta.setUser_info("我是陆星辰，是一个男性，是一位知名导演，也是苏梦远的合作导演。我擅长拍摄音乐题材的电影。苏梦远对我的态度是尊敬的，并视我为良师益友。");
        meta.setBot_info("苏梦远，本名苏远心，是一位当红的国内女歌手及演员。在参加选秀节目后，凭借独特的嗓音及出众的舞台魅力迅速成名，进入娱乐圈。她外表美丽动人，但真正的魅力在于她的才华和勤奋。苏梦远是音乐学院毕业的优秀生，善于创作，拥有多首热门原创歌曲。除了音乐方面的成就，她还热衷于慈善事业，积极参加公益活动，用实际行动传递正能量。在工作中，她对待工作非常敬业，拍戏时总是全身心投入角色，赢得了业内人士的赞誉和粉丝的喜爱。虽然在娱乐圈，但她始终保持低调、谦逊的态度，深得同行尊重。在表达时，苏梦远喜欢使用“我们”和“一起”，强调团队精神。");
        meta.setBot_name("苏梦远");
        meta.setUser_name("陆星辰");

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelCharGLM3)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .meta(meta)
                .extraJson(extraJson)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        logger.info("model output: {}", mapper.writeValueAsString(invokeModelApiResp));
    }

    /**
     * V4异步调用
     */
    @Test
    public void testAsyncInvoke() throws JsonProcessingException {
        String taskId = getAsyncTaskId();
        testQueryResult(taskId);
    }

//

    /**
     * 文生图
     */
    @Test
    public void testCreateImage() throws JsonProcessingException {
        CreateImageRequest createImageRequest = new CreateImageRequest();
        createImageRequest.setModel(Constants.ModelCogView);
        createImageRequest.setPrompt("Futuristic cloud data center, showcasing advanced technologgy and a high-tech atmosp\n" +
                "here. The image should depict a spacious, well-lit interior with rows of server racks, glo\n" +
                "wing lights, and digital displays. Include abstract representattions of data streams and\n" +
                "onnectivity, symbolizing the essence of cloud computing. Thee style should be modern a\n" +
                "nd sleek, with a focus on creating a sense of innovaticon and cutting-edge technology\n" +
                "The overall ambiance should convey the power and effciency of cloud services in a visu\n" +
                "ally engaging way.");
        createImageRequest.setRequestId("test11111111111111");
        ImageApiResponse imageApiResponse = client.createImage(createImageRequest);
        logger.info("imageApiResponse: {}", mapper.writeValueAsString(imageApiResponse));
    }

//
//    /**
//     * 图生文
//     */
//    @Test
//    public void testImageToWord() throws JsonProcessingException {
//        List<ChatMessage> messages = new ArrayList<>();
//        List<Map<String, Object>> contentList = new ArrayList<>();
//        Map<String, Object> textMap = new HashMap<>();
//        textMap.put("type", "text");
//        textMap.put("text", "图里有什么");
//        Map<String, Object> typeMap = new HashMap<>();
//        typeMap.put("type", "image_url");
//        Map<String, Object> urlMap = new HashMap<>();
//        urlMap.put("url", "https://sfile.chatglm.cn/testpath/275ae5b6-5390-51ca-a81a-60332d1a7cac_0.png");
//        typeMap.put("image_url", urlMap);
//        contentList.add(textMap);
//        contentList.add(typeMap);
//        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), contentList);
//        messages.add(chatMessage);
//        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
//
//
//        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
//                .model(Constants.ModelChatGLM4V)
//                .stream(Boolean.FALSE)
//                .invokeMethod(Constants.invokeMethod)
//                .messages(messages)
//                .requestId(requestId)
//                .build();
//        ModelApiResponse modelApiResponse = client.invokeModelApi(chatCompletionRequest);
//        logger.info("model output: {}", mapper.writeValueAsString(modelApiResponse));
//    }
//

    /**
     * 向量模型V4
     */
    @Test
    public void testEmbeddings() throws JsonProcessingException {
        EmbeddingRequest embeddingRequest = new EmbeddingRequest();
        embeddingRequest.setInput("hello world");
        embeddingRequest.setModel(Constants.ModelEmbedding2);
        EmbeddingApiResponse apiResponse = client.invokeEmbeddingsApi(embeddingRequest);
        logger.info("model output: {}", mapper.writeValueAsString(apiResponse));
    }


    /**
     * V4微调上传数据集
     */
    @Test
    public void testUploadFile() throws JsonProcessingException {
        String filePath = "demo.jsonl";

        String path = ClassLoader.getSystemResource(filePath).getPath();
        String purpose = "fine-tune";
        UploadFileRequest request = UploadFileRequest.builder()
                .purpose(purpose)
                .filePath(path)
                .build();

        FileApiResponse fileApiResponse = client.invokeUploadFileApi(request);
        logger.info("model output: {}", mapper.writeValueAsString(fileApiResponse));
    }


    /**
     * 微调V4-查询上传文件列表
     */
    @Test
    public void testQueryUploadFileList() throws JsonProcessingException {
        QueryFilesRequest queryFilesRequest = new QueryFilesRequest();
        QueryFileApiResponse queryFileApiResponse = client.queryFilesApi(queryFilesRequest);
        logger.info("model output: {}", mapper.writeValueAsString(queryFileApiResponse));
    }

    @Test
    public void testFileContent() throws IOException {
        try {

            HttpxBinaryResponseContent httpxBinaryResponseContent = client.fileContent("20240514_ea19d21b-d256-4586-b0df-e80a45e3c286");
            String filePath = "demo_output.jsonl";
            String resourcePath = V4Test.class.getClassLoader().getResource("").getPath();

            httpxBinaryResponseContent.streamToFile(resourcePath + "1" + filePath, 1000);

        } catch (IOException e) {
            logger.error("file content error", e);
        }
    }

////    @Test
////    public void deletedFile() throws IOException {
////        FileDelResponse fileDelResponse = client.deletedFile("20240514_ea19d21b-d256-4586-b0df-e80a45e3c286");
////
////        logger.info("model output: {}", mapper.writeValueAsString(fileDelResponse));
////
////    }
//
//

    /**
     * 微调V4-创建微调任务
     */
    @Test
    public void testCreateFineTuningJob() throws JsonProcessingException {
        FineTuningJobRequest request = new FineTuningJobRequest();
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        request.setRequestId(requestId);
        request.setModel("chatglm3-6b");
        request.setTraining_file("file-20240118082608327-kp8qr");
        CreateFineTuningJobApiResponse createFineTuningJobApiResponse = client.createFineTuningJob(request);
        logger.info("model output: {}", mapper.writeValueAsString(createFineTuningJobApiResponse));
    }


    /**
     * 微调V4-查询微调任务
     */
    @Test
    public void testRetrieveFineTuningJobs() throws JsonProcessingException {
        QueryFineTuningJobRequest queryFineTuningJobRequest = new QueryFineTuningJobRequest();
        queryFineTuningJobRequest.setJobId("ftjob-20240429172916475-fb7r9");
//        queryFineTuningJobRequest.setLimit(1);
//        queryFineTuningJobRequest.setAfter(1);
        QueryFineTuningJobApiResponse queryFineTuningJobApiResponse = client.retrieveFineTuningJobs(queryFineTuningJobRequest);
        logger.info("model output: {}", mapper.writeValueAsString(queryFineTuningJobApiResponse));
    }


    /**
     * 微调V4-查询微调任务
     */
    @Test
    public void testFueryFineTuningJobsEvents() throws JsonProcessingException {
        QueryFineTuningJobRequest queryFineTuningJobRequest = new QueryFineTuningJobRequest();
        queryFineTuningJobRequest.setJobId("ftjob-20240429172916475-fb7r9");

        QueryFineTuningEventApiResponse queryFineTuningEventApiResponse = client.queryFineTuningJobsEvents(queryFineTuningJobRequest);
        logger.info("model output: {}", mapper.writeValueAsString(queryFineTuningEventApiResponse));
    }


    /**
     * testQueryPersonalFineTuningJobs V4-查询个人微调任务
     */
    @Test
    public void testQueryPersonalFineTuningJobs() throws JsonProcessingException {
        QueryPersonalFineTuningJobRequest queryPersonalFineTuningJobRequest = new QueryPersonalFineTuningJobRequest();
        queryPersonalFineTuningJobRequest.setLimit(1);
        QueryPersonalFineTuningJobApiResponse queryPersonalFineTuningJobApiResponse = client.queryPersonalFineTuningJobs(queryPersonalFineTuningJobRequest);
        logger.info("model output: {}", mapper.writeValueAsString(queryPersonalFineTuningJobApiResponse));
    }


    @Test
    public void testBatchesCreate() {
        BatchCreateParams batchCreateParams = new BatchCreateParams(
                "24h",
                "/v4/chat/completions",
                "20240514_ea19d21b-d256-4586-b0df-e80a45e3c286",
                new HashMap<String, String>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }}
        );

        BatchResponse batchResponse = client.batchesCreate(batchCreateParams);
        logger.info("output: {}", batchResponse);
//         output: BatchResponse(code=200, msg=调用成功, success=true, data=Batch(id=batch_1791021399316246528, completionWindow=24h, createdAt=1715847751822, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=validating, cancelledAt=null, cancellingAt=null, completedAt=null, errorFileId=null, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=null, inProgressAt=null, metadata={key1=value1, key2=value2}, outputFileId=null, requestCounts=BatchRequestCounts(completed=0, failed=0, total=0), error=null))
    }

    @Test
    public void testDeleteFineTuningJob() {
        FineTuningJobIdRequest request = FineTuningJobIdRequest.builder().jobId("test").build();
        QueryFineTuningJobApiResponse queryFineTuningJobApiResponse = client.deleteFineTuningJob(request);
        logger.info("output: {}", queryFineTuningJobApiResponse);

    }

    @Test
    public void testCancelFineTuningJob() {
        FineTuningJobIdRequest request = FineTuningJobIdRequest.builder().jobId("test").build();
        QueryFineTuningJobApiResponse queryFineTuningJobApiResponse = client.cancelFineTuningJob(request);
        logger.info("output: {}", queryFineTuningJobApiResponse);

    }

    @Test
    public void testBatchesRetrieve() {
        BatchResponse batchResponse = client.batchesRetrieve("batch_1791021399316246528");
        logger.info("output: {}", batchResponse);

    }

    @Test
    public void testDeleteFineTuningModel() {
        FineTuningJobModelRequest request = FineTuningJobModelRequest.builder().fineTunedModel("test").build();

        FineTunedModelsStatusResponse fineTunedModelsStatusResponse = client.deleteFineTuningModel(request);
        logger.info("output: {}", fineTunedModelsStatusResponse);
//        output: BatchResponse(code=200, msg=调用成功, success=true, data=Batch(id=batch_1791021399316246528, completionWindow=24h, createdAt=1715847752000, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=validating, cancelledAt=null, cancellingAt=null, completedAt=null, errorFileId=, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=null, inProgressAt=null, metadata={key1=value1, key2=value2}, outputFileId=, requestCounts=BatchRequestCounts(completed=0, failed=0, total=0), error=null))

    }

    @Test
    public void testBatchesList() {
        QueryBatchRequest queryBatchRequest = new QueryBatchRequest();
        queryBatchRequest.setLimit(10);
        QueryBatchResponse queryBatchResponse = client.batchesList(queryBatchRequest);
        logger.info("output: {}", queryBatchResponse);
// output: QueryBatchResponse(code=200, msg=调用成功, success=true, data=BatchPage(object=list, data=[Batch(id=batch_1790291013237211136, completionWindow=24h, createdAt=1715673614000, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=completed, cancelledAt=null, cancellingAt=1715673699000, completedAt=null, errorFileId=, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=null, inProgressAt=null, metadata={description=job test}, outputFileId=, requestCounts=BatchRequestCounts(completed=0, failed=0, total=0), error=null), Batch(id=batch_1790292763050508288, completionWindow=24h, createdAt=1715674031000, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=completed, cancelledAt=null, cancellingAt=null, completedAt=1715766416000, errorFileId=, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=1715754569000, inProgressAt=null, metadata={description=job test}, outputFileId=1715766415_e5a77222855a406ca8a082de28549c99, requestCounts=BatchRequestCounts(completed=2, failed=0, total=2), error=null), Batch(id=batch_1791021114887909376, completionWindow=24h, createdAt=1715847684000, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=validating, cancelledAt=null, cancellingAt=null, completedAt=null, errorFileId=, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=null, inProgressAt=null, metadata={key1=value1, key2=value2}, outputFileId=, requestCounts=BatchRequestCounts(completed=0, failed=0, total=0), error=null), Batch(id=batch_1791021399316246528, completionWindow=24h, createdAt=1715847752000, endpoint=/v4/chat/completions, inputFileId=20240514_ea19d21b-d256-4586-b0df-e80a45e3c286, object=batch, status=validating, cancelledAt=null, cancellingAt=null, completedAt=null, errorFileId=, errors=null, expiredAt=null, expiresAt=null, failedAt=null, finalizingAt=null, inProgressAt=null, metadata={key1=value1, key2=value2}, outputFileId=, requestCounts=BatchRequestCounts(completed=0, failed=0, total=0), error=null)], error=null))

    }

    @Test
    public void testBatchesCancel() throws JsonProcessingException {
        getAsyncTaskId();
    }

    private static String getAsyncTaskId() throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "ChatGLM和你哪个更强大");
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
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethodAsync)
                .messages(messages)
                .requestId(requestId)
                .tools(chatToolList)
                .toolChoice("auto")
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        logger.info("model output: {}", mapper.writeValueAsString(invokeModelApiResp));
        return invokeModelApiResp.getData().getId();
    }


    private static void testQueryResult(String taskId) throws JsonProcessingException {
        QueryModelResultRequest request = new QueryModelResultRequest();
        request.setTaskId(taskId);
        QueryModelResultResponse queryResultResp = client.queryModelResult(request);
        logger.info("model output {}", mapper.writeValueAsString(queryResultResp));
    }

    public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> {
            return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
        });
    }
}
