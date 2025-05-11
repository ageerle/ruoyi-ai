package org.ruoyi.common.chat.openai;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.ruoyi.common.chat.constant.OpenAIConst;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.billing.BillingUsage;
import org.ruoyi.common.chat.entity.billing.KeyInfo;
import org.ruoyi.common.chat.entity.billing.Subscription;
import org.ruoyi.common.chat.entity.chat.*;
import org.ruoyi.common.chat.entity.embeddings.Embedding;
import org.ruoyi.common.chat.entity.embeddings.EmbeddingResponse;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.images.Image;
import org.ruoyi.common.chat.entity.images.ImageResponse;
import org.ruoyi.common.chat.entity.models.Model;
import org.ruoyi.common.chat.entity.models.ModelResponse;
import org.ruoyi.common.chat.entity.whisper.Transcriptions;
import org.ruoyi.common.chat.entity.whisper.Translations;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.exception.CommonError;
import org.ruoyi.common.chat.openai.function.KeyRandomStrategy;
import org.ruoyi.common.chat.openai.function.KeyStrategyFunction;
import org.ruoyi.common.chat.openai.interceptor.DefaultOpenAiAuthInterceptor;
import org.ruoyi.common.chat.openai.interceptor.DynamicKeyOpenAiAuthInterceptor;
import org.ruoyi.common.chat.openai.interceptor.OpenAiAuthInterceptor;
import org.ruoyi.common.chat.openai.plugin.PluginAbstract;
import org.ruoyi.common.chat.openai.plugin.PluginParam;
import org.ruoyi.common.chat.sse.DefaultPluginListener;
import org.ruoyi.common.chat.sse.PluginListener;
import org.ruoyi.common.core.exception.base.BaseException;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *   open ai 客户端
 *
 * @author https:www.unfbx.com
 * 2023-02-28
 */

@Getter
@Slf4j
@Setter
public class OpenAiStreamClient {

    @NotNull
    private List<String> apiKey;
    /**
     * 自定义api host使用builder的方式构造client
     */
    private String apiHost;

    /**
     * 自定义url 兼容多个平台
     */
    private String apiUrl;

    /**
     * 自定义的okHttpClient
     * 如果不自定义 ，就是用sdk默认的OkHttpClient实例
     */
    private OkHttpClient okHttpClient;

    /**
     * api key的获取策略
     */
    private KeyStrategyFunction<List<String>, String> keyStrategy;

    private OpenAiApi openAiApi;

    /**
     * 自定义鉴权处理拦截器<br/>
     * 可以不设置，默认实现：DefaultOpenAiAuthInterceptor <br/>
     * 如需自定义实现参考：DealKeyWithOpenAiAuthInterceptor
     *
     * @see DynamicKeyOpenAiAuthInterceptor
     * @see DefaultOpenAiAuthInterceptor
     */
    private OpenAiAuthInterceptor authInterceptor;

    private static final String DONE_SIGNAL = "[DONE]";

    /**
     * 构造实例对象
     *
     * @param builder
     */
    private OpenAiStreamClient(Builder builder) {
        if (CollectionUtil.isEmpty(builder.apiKey)) {
            throw new BaseException(CommonError.API_KEYS_NOT_NUL.msg());
        }
        apiKey = builder.apiKey;

        if (StrUtil.isBlank(builder.apiHost)) {
            builder.apiHost = OpenAIConst.OPENAI_HOST;
        }
        apiHost = builder.apiHost;

        if (StrUtil.isBlank(builder.apiUrl)) {
            builder.apiUrl = OpenAIConst.apiUrl;
        }
        apiUrl = builder.apiUrl;

        if (Objects.isNull(builder.keyStrategy)) {
            builder.keyStrategy = new KeyRandomStrategy();
        }
        keyStrategy = builder.keyStrategy;

        if (Objects.isNull(builder.authInterceptor)) {
            builder.authInterceptor = new DefaultOpenAiAuthInterceptor();
        }
        authInterceptor = builder.authInterceptor;
        //设置apiKeys和key的获取策略
        authInterceptor.setApiKey(this.apiKey);
        authInterceptor.setKeyStrategy(this.keyStrategy);

        if (Objects.isNull(builder.okHttpClient)) {
            builder.okHttpClient = this.okHttpClient();
        } else {
            //自定义的okhttpClient  需要增加api keys
            builder.okHttpClient = builder.okHttpClient
                .newBuilder()
                .addInterceptor(authInterceptor)
                .build();
        }
        okHttpClient = builder.okHttpClient;

        this.openAiApi = new Retrofit.Builder()
            .baseUrl(apiHost)
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create())
            .build().create(OpenAiApi.class);
    }

    /**
     * 创建默认的OkHttpClient
     */
    private OkHttpClient okHttpClient() {
        if (Objects.isNull(this.authInterceptor)) {
            this.authInterceptor = new DefaultOpenAiAuthInterceptor();
        }
        this.authInterceptor.setApiKey(this.apiKey);
        this.authInterceptor.setKeyStrategy(this.keyStrategy);
        OkHttpClient okHttpClient = new OkHttpClient
            .Builder()
            .addInterceptor(this.authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .build();
        return okHttpClient;
    }


    /**
     * 流式输出，最新版的GPT-3.5 chat completion 更加贴近官方网站的问答模型
     *
     * @param chatCompletion      问答参数
     * @param eventSourceListener 监听器
     */
    public <T extends BaseChatCompletion> void streamChatCompletion(T chatCompletion, EventSourceListener eventSourceListener) {
        if (Objects.isNull(eventSourceListener)) {
            log.error("参数异常：EventSourceListener不能为空!");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
        try {
            EventSource.Factory factory = EventSources.createFactory(this.okHttpClient);
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(chatCompletion);
            Request request = new Request.Builder()
                .url(this.apiHost)
                .post(RequestBody.create(MediaType.parse(ContentType.JSON.getValue()), requestBody))
                .build();
            factory.newEventSource(request, eventSourceListener);
        } catch (Exception e) {
            log.error("请求参数解析异常：{}", e.getMessage());
        }
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param chatCompletion            参数
     * @param eventSourceListener       sse监听器
     * @param pluginEventSourceListener 插件sse监听器，收集function call返回信息
     * @param plugin                    插件
     * @param <R>                       插件自定义函数的请求值
     * @param <T>                       插件自定义函数的返回值
     */
    public <R extends PluginParam, T> void streamChatCompletionWithPlugin(ChatCompletion chatCompletion, EventSourceListener eventSourceListener, PluginListener pluginEventSourceListener, PluginAbstract<R, T> plugin) {
        if (Objects.isNull(plugin)) {
            this.streamChatCompletion(chatCompletion, eventSourceListener);
            return;
        }
        if (CollectionUtil.isEmpty(chatCompletion.getMessages())) {
            throw new BaseException(CommonError.MESSAGE_NOT_NUL.msg());
        }
        Functions functions = Functions.builder()
                .name(plugin.getFunction())
                .description(plugin.getDescription())
                .parameters(plugin.getParameters())
                .build();
        //没有值，设置默认值
        if (Objects.isNull(chatCompletion.getFunctionCall())) {
            chatCompletion.setFunctionCall("auto");
        }
        //tip: 覆盖自己设置的functions参数，使用plugin构造的functions
        chatCompletion.setFunctions(Collections.singletonList(functions));
        //调用OpenAi
        if (Objects.isNull(pluginEventSourceListener)) {
            pluginEventSourceListener = new DefaultPluginListener(this, eventSourceListener, plugin, chatCompletion);
        }
        this.streamChatCompletion(chatCompletion, pluginEventSourceListener);
    }


    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param chatCompletion      参数
     * @param eventSourceListener sse监听器
     * @param plugin              插件
     * @param <R>                 插件自定义函数的请求值
     * @param <T>                 插件自定义函数的返回值
     */
    public <R extends PluginParam, T> void streamChatCompletionWithPlugin(ChatCompletion chatCompletion, EventSourceListener eventSourceListener, PluginAbstract<R, T> plugin) {
        PluginListener pluginEventSourceListener = new DefaultPluginListener(this, eventSourceListener, plugin, chatCompletion);
        this.streamChatCompletionWithPlugin(chatCompletion, eventSourceListener, pluginEventSourceListener, plugin);
    }


    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param messages            问答参数
     * @param eventSourceListener sse监听器
     * @param plugin              插件
     * @param <R>                 插件自定义函数的请求值
     * @param <T>                 插件自定义函数的返回值
     */
    public <R extends PluginParam, T> void streamChatCompletionWithPlugin(List<Message> messages, EventSourceListener eventSourceListener, PluginAbstract<R, T> plugin) {
        this.streamChatCompletionWithPlugin(messages, ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName(), eventSourceListener, plugin);
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     *
     * @param messages            问答参数
     * @param model               模型
     * @param eventSourceListener eventSourceListener
     * @param plugin              插件
     * @param <R>                 插件自定义函数的请求值
     * @param <T>                 插件自定义函数的返回值
     */
    public <R extends PluginParam, T> void streamChatCompletionWithPlugin(List<Message> messages, String model, EventSourceListener eventSourceListener, PluginAbstract<R, T> plugin) {
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).model(model).build();
        this.streamChatCompletionWithPlugin(chatCompletion, eventSourceListener, plugin);
    }


    /**
     * 根据描述生成图片
     *
     * @param image 图片参数
     * @return ImageResponse
     */
    public ImageResponse genImages(Image image) {
        Single<ImageResponse> edits = this.openAiApi.genImages(image);
        return edits.blockingGet();
    }

    /**
     * 最新版的GPT-3.5 chat completion 更加贴近官方网站的问答模型
     *
     * @param chatCompletion 问答参数
     * @return 答案
     */
    public <T extends BaseChatCompletion> ChatCompletionResponse chatCompletion(T chatCompletion) {
        if (chatCompletion instanceof ChatCompletion) {
            Single<ChatCompletionResponse> chatCompletionResponse = this.openAiApi.chatCompletion((ChatCompletion) chatCompletion);
            return chatCompletionResponse.blockingGet();
        }
        Single<ChatCompletionResponse> chatCompletionResponse = this.openAiApi.chatCompletionWithPicture((ChatCompletionWithPicture) chatCompletion);
        return chatCompletionResponse.blockingGet();
    }

    /**
     * 上传文件
     *
     * @param purpose purpose
     * @param file    文件对象
     * @return UploadFileResponse
     */
    public UploadFileResponse uploadFile(String purpose, java.io.File file) {
        // 创建 RequestBody，用于封装构建RequestBody
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        RequestBody purposeBody = RequestBody.create(MediaType.parse("multipart/form-data"), purpose);
        Single<UploadFileResponse> uploadFileResponse = this.openAiApi.uploadFile(multipartBody, purposeBody);
        return uploadFileResponse.blockingGet();
    }

    /**
     * 获取openKey账户信息(近90天)
     *
     * @param key
     * @return KeyInfo
     * @Date 2023/7/6
     **/
    public KeyInfo getKeyInfo(String key) {
        Date now = new Date();
        Date start = new Date(now.getTime() - (long) 90 * 24 * 60 * 60 * 1000);
        Date end = new Date(now.getTime() + (long) 24 * 60 * 60 * 1000);

        BillingUsage billingUsage = billingUsage(start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        double totalUsage = billingUsage.getTotalUsage().doubleValue() / 100;
        Subscription subscription = subscription();
        KeyInfo keyInfo = new KeyInfo();
        String start_key = key.substring(0, 6);
        String end_key = key.substring(key.length() - 6);
        String mid_key = key.substring(6, key.length() - 6);
        mid_key = mid_key.replaceAll(".", "*");

        keyInfo.setKeyValue(start_key + mid_key + end_key);
        keyInfo.setTotalAmount(subscription.getHardLimitUsd());
        keyInfo.setRemaining(subscription.getHardLimitUsd() - totalUsage);
        keyInfo.setTotalUsage(totalUsage);
        keyInfo.setLimitDate(new Date(subscription.getAccessUntil() * 1000).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        keyInfo.setPlanTitle(subscription.getPlan() != null ? subscription.getPlan().getTitle() : "null");
        keyInfo.setIsHasPaymentMethod(subscription.isHasPaymentMethod());
        keyInfo.setModel(getModelName());
        return keyInfo;
    }

    /**
     * 获取可用模型
     *
     * @param
     * @return String
     * @Date 2023/7/6
     **/
    public String getModelName() {
        Single<ModelResponse> models = this.openAiApi.models();
        List<Model> modelList = models.blockingGet().getData();
        for (Model model : modelList) {
            if (Objects.equals(model.getId(), "gpt-4")) {
                return "GPT-4.0";
            }
        }
        return "GPT-3.5";
    }

    /**
     * 账户调用接口消耗金额信息查询
     * 最多查询100天
     *
     * @param starDate 开始时间
     * @param endDate  结束时间
     * @return 消耗金额信息
     */
    public BillingUsage billingUsage(@NotNull LocalDate starDate, @NotNull LocalDate endDate) {
        Single<BillingUsage> billingUsage = this.openAiApi.billingUsage(starDate, endDate);
        return billingUsage.blockingGet();
    }

    /**
     * 文本转换向量
     *
     * @param embedding 入参
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(Embedding embedding) {
        Single<EmbeddingResponse> embeddings = this.openAiApi.embeddings(embedding);
        return embeddings.blockingGet();
    }

    /**
     * 账户信息查询：里面包含总金额等信息
     *
     * @return 账户信息
     */
    public Subscription subscription() {
        Single<Subscription> subscription = this.openAiApi.subscription();
        return subscription.blockingGet();
    }

    /**
     * 语音转文字
     *
     * @param transcriptions 参数
     * @param file           语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 语音文本
     */
    public WhisperResponse speechToTextTranscriptions(java.io.File file, Transcriptions transcriptions) {
        //文件
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
        //自定义参数
        Map<String, RequestBody> requestBodyMap = new HashMap<>(10);
        if (StrUtil.isNotBlank(transcriptions.getLanguage())) {
            requestBodyMap.put(Transcriptions.Fields.language, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getLanguage()));
        }
        if (StrUtil.isNotBlank(transcriptions.getModel())) {
            requestBodyMap.put(Transcriptions.Fields.model, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getModel()));
        }
        if (StrUtil.isNotBlank(transcriptions.getPrompt())) {
            requestBodyMap.put(Transcriptions.Fields.prompt, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getPrompt()));
        }
        if (StrUtil.isNotBlank(transcriptions.getResponseFormat())) {
            requestBodyMap.put(Transcriptions.Fields.responseFormat, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getResponseFormat()));
        }
        if (Objects.nonNull(transcriptions.getTemperature())) {
            requestBodyMap.put(Transcriptions.Fields.temperature, RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(transcriptions.getTemperature())));
        }
        Single<WhisperResponse> whisperResponse = this.openAiApi.speechToTextTranscriptions(multipartBody, requestBodyMap);
        return whisperResponse.blockingGet();
    }

    /**
     * 简易版 语音转文字
     *
     * @param file 语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 语音文本
     */
    public WhisperResponse speechToTextTranscriptions(java.io.File file) {
        Transcriptions transcriptions = Transcriptions.builder().build();
        return this.speechToTextTranscriptions(file, transcriptions);
    }
    /**
     * 文本转语音（异步）
     *
     * @param textToSpeech 参数
     * @param callback     返回值接收
     * @since 1.1.2
     */
    public void textToSpeech(TextToSpeech textToSpeech, retrofit2.Callback callback) {
        Call<ResponseBody> responseBody = this.openAiApi.textToSpeech(textToSpeech);
        responseBody.enqueue(callback);
    }

    /**
     * 文本转语音（同步）
     *
     * @param textToSpeech 参数
     * @since 1.1.3
     */
    public ResponseBody textToSpeech(TextToSpeech textToSpeech){
        try {
            Call<ResponseBody> responseBody = this.openAiApi.textToSpeech(textToSpeech);
            return responseBody.execute().body();
        } catch (IOException e) {
            throw new BaseException("文本转语音（同步）失败: "+e.getMessage());
        }
    }

    /**
     * 文本转语音（克隆）
     *
     * @param textToSpeech
     * @return
     */
    public ResponseBody textToSpeechClone(TextToSpeech textToSpeech) {
        String baseUrl = "http://localhost:8081";
        String spk = "三月七";
        String text = textToSpeech.getInput();
        String lang = "zh";

        // 创建OkHttpClient实例
        OkHttpClient client = new OkHttpClient();

        // 构建请求URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        urlBuilder.addQueryParameter("spk", spk);
        urlBuilder.addQueryParameter("text", text);
        urlBuilder.addQueryParameter("lang", lang);
        String url = urlBuilder.build().toString();

        // 创建请求对象
        Request request = new Request.Builder()
            .url(url)
            .build();
        // 发送请求并处理响应
        try {
            return client.newCall(request).execute().body();
        } catch (IOException e) {
            throw new BaseException("语音克隆失败！{}",e.getMessage());
        }
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param chatCompletion 参数
     * @param plugin         插件
     * @param <R>            插件自定义函数的请求值
     * @param <T>            插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(ChatCompletion chatCompletion, PluginAbstract<R, T> plugin) {
        if (Objects.isNull(plugin)) {
            return this.chatCompletion(chatCompletion);
        }
        if (CollectionUtil.isEmpty(chatCompletion.getMessages())) {
            throw new BaseException(CommonError.MESSAGE_NOT_NUL.msg());
        }
        List<Message> messages = chatCompletion.getMessages();
        Functions functions = Functions.builder()
                .name(plugin.getFunction())
                .description(plugin.getDescription())
                .parameters(plugin.getParameters())
                .build();
        //没有值，设置默认值
        if (Objects.isNull(chatCompletion.getFunctionCall())) {
            chatCompletion.setFunctionCall("auto");
        }
        //tip: 覆盖自己设置的functions参数，使用plugin构造的functions
        chatCompletion.setFunctions(Collections.singletonList(functions));
        //调用OpenAi
        ChatCompletionResponse functionCallChatCompletionResponse = this.chatCompletion(chatCompletion);
        ChatChoice chatChoice = functionCallChatCompletionResponse.getChoices().get(0);
        log.debug("构造的方法值：{}", chatChoice.getMessage().getFunctionCall());

        R realFunctionParam = (R) JSONUtil.toBean(chatChoice.getMessage().getFunctionCall().getArguments(), plugin.getR());
        T tq = plugin.func(realFunctionParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(chatChoice.getMessage().getFunctionCall().getArguments())
                .name(plugin.getFunction())
                .build();
        messages.add(Message.builder().role(Message.Role.ASSISTANT).content("function_call").functionCall(functionCall).build());
        messages.add(Message.builder().role(Message.Role.FUNCTION).name(plugin.getFunction()).content(plugin.content(tq)).build());
        //设置第二次，请求的参数
        chatCompletion.setFunctionCall(null);
        chatCompletion.setFunctions(null);

        ChatCompletionResponse chatCompletionResponse = this.chatCompletion(chatCompletion);
        log.debug("自定义的方法返回值：{}", chatCompletionResponse.getChoices());
        return chatCompletionResponse;
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param messages 问答参数
     * @param plugin   插件
     * @param <R>      插件自定义函数的请求值
     * @param <T>      插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(List<Message> messages, PluginAbstract<R, T> plugin) {
        return chatCompletionWithPlugin(messages, ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName(), plugin);
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     *
     * @param messages 问答参数
     * @param model    模型
     * @param plugin   插件
     * @param <R>      插件自定义函数的请求值
     * @param <T>      插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(List<Message> messages, String model, PluginAbstract<R, T> plugin) {
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).model(model).build();
        return this.chatCompletionWithPlugin(chatCompletion, plugin);
    }


    /**
     * 构造
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private @NotNull List<String> apiKey;
        /**
         * api请求地址，结尾处有斜杠
         *
         * @see OpenAIConst
         */
        private String apiHost;

        private String apiUrl;

        /**
         * 自定义OkhttpClient
         */
        private OkHttpClient okHttpClient;


        /**
         * api key的获取策略
         */
        private KeyStrategyFunction keyStrategy;

        /**
         * 自定义鉴权拦截器
         */
        private OpenAiAuthInterceptor authInterceptor;

        public Builder() {
        }

        public Builder apiKey(@NotNull List<String> val) {
            apiKey = val;
            return this;
        }

        /**
         * @param val api请求地址，结尾处有斜杠
         * @return Builder
         * @see OpenAIConst
         */
        public Builder apiHost(String val) {
            apiHost = val;
            return this;
        }

        /**
         * @param val 自定义请求后缀
         * @return Builder
         * @see OpenAIConst
         */
        public Builder apiUrl(String val) {
            apiUrl = val;
            return this;
        }

        public Builder keyStrategy(KeyStrategyFunction val) {
            keyStrategy = val;
            return this;
        }

        public Builder okHttpClient(OkHttpClient val) {
            okHttpClient = val;
            return this;
        }

        public Builder authInterceptor(OpenAiAuthInterceptor val) {
            authInterceptor = val;
            return this;
        }

        public OpenAiStreamClient build() {
            return new OpenAiStreamClient(this);
        }
    }
}
