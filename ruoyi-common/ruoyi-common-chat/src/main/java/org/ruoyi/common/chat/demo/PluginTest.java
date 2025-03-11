package org.ruoyi.common.chat.demo;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.ruoyi.common.chat.entity.chat.*;
import org.ruoyi.common.chat.entity.chat.tool.ToolCallFunction;
import org.ruoyi.common.chat.entity.chat.tool.ToolCalls;
import org.ruoyi.common.chat.entity.chat.tool.Tools;
import org.ruoyi.common.chat.entity.chat.tool.ToolsFunction;
import org.ruoyi.common.chat.openai.OpenAiClient;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.openai.function.KeyRandomStrategy;
import org.ruoyi.common.chat.openai.interceptor.DynamicKeyOpenAiAuthInterceptor;
import org.ruoyi.common.chat.openai.interceptor.OpenAILogger;
import org.ruoyi.common.chat.openai.interceptor.OpenAiResponseInterceptor;
import org.ruoyi.common.chat.openai.plugin.PluginAbstract;
import org.ruoyi.common.chat.plugin.CmdPlugin;
import org.ruoyi.common.chat.plugin.CmdReq;
import org.ruoyi.common.chat.sse.ConsoleEventSourceListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 描述：
 *
 * @author ageerle@163.com
 * date 2025/3/8
 */
@Slf4j
public class PluginTest {

    private OpenAiClient openAiClient;
    private OpenAiStreamClient openAiStreamClient;

    @Before
    public void before() {
        //可以为null
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        //！！！！千万别再生产或者测试环境打开BODY级别日志！！！！
        //！！！生产或者测试环境建议设置为这三种级别：NONE,BASIC,HEADERS,！！！
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
//                .proxy(proxy)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new OpenAiResponseInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        openAiClient = OpenAiClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Arrays.asList("sk-xx"))
                //自定义key的获取策略：默认KeyRandomStrategy
                //.keyStrategy(new KeyRandomStrategy())
                .keyStrategy(new KeyRandomStrategy())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://api.pandarobot.chat/")
                .build();

        openAiStreamClient = OpenAiStreamClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Arrays.asList("sk-xx"))
                //自定义key的获取策略：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .authInterceptor(new DynamicKeyOpenAiAuthInterceptor())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://api.pandarobot.chat/")
                .build();
    }


    @Test
    public void chatFunction() {
        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER).content("给我输出一个长度为2的中文词语，并解释下词语对应物品的用途").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.put("type", "number");
        wordLength.put("description", "词语的长度");
        //属性二
        JSONObject language = new JSONObject();
        language.put("type", "string");
        language.put("enum", Arrays.asList("zh", "en"));
        language.put("description", "语言类型，例如：zh代表中文、en代表英语");
        //参数
        JSONObject properties = new JSONObject();
        properties.put("wordLength", wordLength);
        properties.put("language", language);

        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Collections.singletonList("wordLength")).build();
        Functions functions = Functions.builder()
                .name("getOneWord")
                .description("获取一个指定长度和语言类型的词语")
                .parameters(parameters)
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .functions(Collections.singletonList(functions))
                .functionCall("auto")
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);

        ChatChoice chatChoice = chatCompletionResponse.getChoices().get(0);
        log.info("构造的方法值：{}", chatChoice.getMessage().getFunctionCall());
        log.info("构造的方法名称：{}", chatChoice.getMessage().getFunctionCall().getName());
        log.info("构造的方法参数：{}", chatChoice.getMessage().getFunctionCall().getArguments());
        WordParam wordParam = JSONUtil.toBean(chatChoice.getMessage().getFunctionCall().getArguments(), WordParam.class);
        String oneWord = getOneWord(wordParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(chatChoice.getMessage().getFunctionCall().getArguments())
                .name("getOneWord")
                .build();
        Message message2 = Message.builder().role(Message.Role.ASSISTANT).content("方法参数").functionCall(functionCall).build();
        String content
                = "{ " +
                "\"wordLength\": \"3\", " +
                "\"language\": \"zh\", " +
                "\"word\": \"" + oneWord + "\"," +
                "\"用途\": [\"直接吃\", \"做沙拉\", \"售卖\"]" +
                "}";
        Message message3 = Message.builder().role(Message.Role.FUNCTION).name("getOneWord").content(content).build();
        List<Message> messageList = Arrays.asList(message, message2, message3);
        ChatCompletion chatCompletionV2 = ChatCompletion
                .builder()
                .messages(messageList)
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        ChatCompletionResponse chatCompletionResponseV2 = openAiClient.chatCompletion(chatCompletionV2);
        log.info("自定义的方法返回值：{}",chatCompletionResponseV2.getChoices().get(0).getMessage().getContent());
    }


    @Test
    public void plugin() {
        CmdPlugin plugin = new CmdPlugin(CmdReq.class);
        // 插件名称
        plugin.setName("命令行工具");
        // 方法名称
        plugin.setFunction("openCmd");
        // 方法说明
        plugin.setDescription("提供一个命令行指令,比如<记事本>,指令使用中文,以function返回结果为准");

        PluginAbstract.Arg arg = new PluginAbstract.Arg();
        // 参数名称
        arg.setName("cmd");
        // 参数说明
        arg.setDescription("命令行指令");
        // 参数类型
        arg.setType("string");
        arg.setRequired(true);
        plugin.setArgs(Collections.singletonList(arg));

        Message message2 = Message.builder().role(Message.Role.USER).content("帮我打开计算器,结合上下文判断指令是否执行成功,只用回复成功或者失败").build();
        List<Message> messages = new ArrayList<>();
        messages.add(message2);
        //有四个重载方法，都可以使用
        ChatCompletionResponse response = openAiClient.chatCompletionWithPlugin(messages,"gpt-4o-mini",plugin);
        log.info("自定义的方法返回值：{}", response.getChoices().get(0).getMessage().getContent());
    }

    /**
     * 自定义返回数据格式
     */
    @Test
    public void diyReturnDataModelChat() {
        Message message = Message.builder().role(Message.Role.USER).content("随机输出10个单词，使用json输出").build();
        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT.getName()).build())
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
        chatCompletionResponse.getChoices().forEach(e -> System.out.println(e.getMessage()));
    }

    @Test
    public void streamPlugin() {
        WeatherPlugin plugin = new WeatherPlugin(WeatherReq.class);
        plugin.setName("知心天气");
        plugin.setFunction("getLocationWeather");
        plugin.setDescription("提供一个地址，方法将会获取该地址的天气的实时温度信息。");
        PluginAbstract.Arg arg = new PluginAbstract.Arg();
        arg.setName("location");
        arg.setDescription("地名");
        arg.setType("string");
        arg.setRequired(true);
        plugin.setArgs(Collections.singletonList(arg));

//        Message message1 = Message.builder().role(Message.Role.USER).content("秦始皇统一了哪六国。").build();
        Message message2 = Message.builder().role(Message.Role.USER).content("获取上海市的天气现在多少度，然后再给出3个推荐的户外运动。").build();
        List<Message> messages = new ArrayList<>();
//        messages.add(message1);
        messages.add(message2);
        //默认模型：GPT_3_5_TURBO_16K_0613
        //有四个重载方法，都可以使用
        openAiStreamClient.streamChatCompletionWithPlugin(messages, ChatCompletion.Model.GPT_4_1106_PREVIEW.getName(), new ConsoleEventSourceListener(), plugin);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * tools使用示例
     */
    @Test
    public void toolsChat() {
        Message message = Message.builder().role(Message.Role.USER).content("给我输出一个长度为2的中文词语，并解释下词语对应物品的用途").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.put("type", "number");
        wordLength.put("description", "词语的长度");
        //属性二
        JSONObject language = new JSONObject();
        language.put("type", "string");
        language.put("enum", Arrays.asList("zh", "en"));
        language.put("description", "语言类型，例如：zh代表中文、en代表英语");
        //参数
        JSONObject properties = new JSONObject();
        properties.put("wordLength", wordLength);
        properties.put("language", language);
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Collections.singletonList("wordLength")).build();
        Tools tools = Tools.builder()
                .type(Tools.Type.FUNCTION.getName())
                .function(ToolsFunction.builder().name("getOneWord").description("获取一个指定长度和语言类型的词语").parameters(parameters).build())
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .tools(Collections.singletonList(tools))
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);

        ChatChoice chatChoice = chatCompletionResponse.getChoices().get(0);
        log.info("构造的方法值：{}", chatChoice.getMessage().getToolCalls());

        ToolCalls openAiReturnToolCalls = chatChoice.getMessage().getToolCalls().get(0);
        WordParam wordParam = JSONUtil.toBean(openAiReturnToolCalls.getFunction().getArguments(), WordParam.class);
        String oneWord = getOneWord(wordParam);


        ToolCallFunction tcf = ToolCallFunction.builder().name("getOneWord").arguments(openAiReturnToolCalls.getFunction().getArguments()).build();
        ToolCalls tc = ToolCalls.builder().id(openAiReturnToolCalls.getId()).type(ToolCalls.Type.FUNCTION.getName()).function(tcf).build();
        //构造tool call
        Message message2 = Message.builder().role(Message.Role.ASSISTANT).content("方法参数").toolCalls(Collections.singletonList(tc)).build();
        String content
                = "{ " +
                "\"wordLength\": \"3\", " +
                "\"language\": \"zh\", " +
                "\"word\": \"" + oneWord + "\"," +
                "\"用途\": [\"直接吃\", \"做沙拉\", \"售卖\"]" +
                "}";
        Message message3 = Message.builder().toolCallId(openAiReturnToolCalls.getId()).role(Message.Role.TOOL).name("getOneWord").content(content).build();
        List<Message> messageList = Arrays.asList(message, message2, message3);
        ChatCompletion chatCompletionV2 = ChatCompletion
                .builder()
                .messages(messageList)
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        ChatCompletionResponse chatCompletionResponseV2 = openAiClient.chatCompletion(chatCompletionV2);
        log.info("自定义的方法返回值：{}", chatCompletionResponseV2.getChoices().get(0).getMessage().getContent());

    }

    /**
     * tools流式输出使用示例
     */
    @Test
    public void streamToolsChat() {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        ConsoleEventSourceListenerV3 eventSourceListener = new ConsoleEventSourceListenerV3(countDownLatch);

        Message message = Message.builder().role(Message.Role.USER).content("给我输出一个长度为2的中文词语，并解释下词语对应物品的用途").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.put("type", "number");
        wordLength.put("description", "词语的长度");
        //属性二
        JSONObject language = new JSONObject();
        language.put("type", "string");
        language.put("enum", Arrays.asList("zh", "en"));
        language.put("description", "语言类型，例如：zh代表中文、en代表英语");
        //参数
        JSONObject properties = new JSONObject();
        properties.put("wordLength", wordLength);
        properties.put("language", language);
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Collections.singletonList("wordLength")).build();
        Tools tools = Tools.builder()
                .type(Tools.Type.FUNCTION.getName())
                .function(ToolsFunction.builder().name("getOneWord").description("获取一个指定长度和语言类型的词语").parameters(parameters).build())
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .tools(Collections.singletonList(tools))
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();
        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ToolCalls openAiReturnToolCalls = eventSourceListener.getToolCalls();
        WordParam wordParam = JSONUtil.toBean(openAiReturnToolCalls.getFunction().getArguments(), WordParam.class);
        String oneWord = getOneWord(wordParam);


        ToolCallFunction tcf = ToolCallFunction.builder().name("getOneWord").arguments(openAiReturnToolCalls.getFunction().getArguments()).build();
        ToolCalls tc = ToolCalls.builder().id(openAiReturnToolCalls.getId()).type(ToolCalls.Type.FUNCTION.getName()).function(tcf).build();
        //构造tool call
        Message message2 = Message.builder().role(Message.Role.ASSISTANT).content("方法参数").toolCalls(Collections.singletonList(tc)).build();
        String content
                = "{ " +
                "\"wordLength\": \"3\", " +
                "\"language\": \"zh\", " +
                "\"word\": \"" + oneWord + "\"," +
                "\"用途\": [\"直接吃\", \"做沙拉\", \"售卖\"]" +
                "}";
        Message message3 = Message.builder().toolCallId(openAiReturnToolCalls.getId()).role(Message.Role.TOOL).name("getOneWord").content(content).build();
        List<Message> messageList = Arrays.asList(message, message2, message3);
        ChatCompletion chatCompletionV2 = ChatCompletion
                .builder()
                .messages(messageList)
                .model(ChatCompletion.Model.GPT_4_1106_PREVIEW.getName())
                .build();


        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        openAiStreamClient.streamChatCompletion(chatCompletionV2, new ConsoleEventSourceListenerV3(countDownLatch));
        try {
            countDownLatch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            countDownLatch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @Data
    @Builder
    static class WordParam {
        private int wordLength;
        @Builder.Default
        private String language = "zh";
    }


    /**
     * 获取一个词语(根据语言和字符长度查询)
     * @param wordParam
     * @return
     */
    public String getOneWord(WordParam wordParam) {

        List<String> zh = Arrays.asList("大香蕉", "哈密瓜", "苹果");
        List<String> en = Arrays.asList("apple", "banana", "cantaloupe");
        if (wordParam.getLanguage().equals("zh")) {
            for (String e : zh) {
                if (e.length() == wordParam.getWordLength()) {
                    return e;
                }
            }
        }
        if (wordParam.getLanguage().equals("en")) {
            for (String e : en) {
                if (e.length() == wordParam.getWordLength()) {
                    return e;
                }
            }
        }
        return "西瓜";
    }
}
