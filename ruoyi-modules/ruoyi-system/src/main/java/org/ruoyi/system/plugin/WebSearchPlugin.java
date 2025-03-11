package org.ruoyi.system.plugin;


import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.ruoyi.common.chat.demo.ConsoleEventSourceListenerV3;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.chat.Parameters;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebSearchPlugin {

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
                .apiKey(Arrays.asList("xx"))
                //自定义key的获取策略：默认KeyRandomStrategy
                //.keyStrategy(new KeyRandomStrategy())
                .keyStrategy(new KeyRandomStrategy())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://open.bigmodel.cn/")
                .build();

        openAiStreamClient = OpenAiStreamClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Arrays.asList("xx"))
                //自定义key的获取策略：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .authInterceptor(new DynamicKeyOpenAiAuthInterceptor())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://open.bigmodel.cn/")
                .build();
    }

    @Test
    public void test() {
        Message message = Message.builder().role(Message.Role.USER).content("今天武汉天气怎么样").build();
        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
//                .tools(Collections.singletonList(tools))
                .model("web-search-pro")
                .build();
        ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);

        System.out.printf("chatCompletionResponse=%s\n", JSONUtil.toJsonStr(chatCompletionResponse));
    }

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
