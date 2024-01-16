package com.xmzs.common.chat.openai;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.entity.billing.BillingUsage;
import com.xmzs.common.chat.entity.billing.KeyInfo;
import com.xmzs.common.chat.entity.billing.Subscription;
import com.xmzs.common.chat.entity.chat.BaseChatCompletion;
import com.xmzs.common.chat.entity.chat.ChatCompletionResponse;
import com.xmzs.common.chat.entity.chat.ChatCompletionWithPicture;
import com.xmzs.common.chat.entity.images.Image;
import com.xmzs.common.chat.entity.images.ImageResponse;
import com.xmzs.common.chat.entity.models.Model;
import com.xmzs.common.chat.entity.models.ModelResponse;
import com.xmzs.common.chat.entity.whisper.Transcriptions;
import com.xmzs.common.chat.entity.whisper.WhisperResponse;
import com.xmzs.common.chat.openai.exception.CommonError;
import com.xmzs.common.chat.openai.function.KeyRandomStrategy;
import com.xmzs.common.chat.openai.function.KeyStrategyFunction;
import com.xmzs.common.chat.openai.interceptor.DefaultOpenAiAuthInterceptor;
import com.xmzs.common.chat.openai.interceptor.DynamicKeyOpenAiAuthInterceptor;
import com.xmzs.common.chat.openai.interceptor.OpenAiAuthInterceptor;
import com.xmzs.common.core.exception.ServiceException;
import io.reactivex.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.chat.entity.chat.ChatCompletion;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.rmi.ServerException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 描述： open ai 客户端
 *
 * @author https:www.unfbx.com
 * 2023-02-28
 */

@Slf4j
public class OpenAiStreamClient {
    @Getter
    @NotNull
    private List<String> apiKey;
    /**
     * 自定义api host使用builder的方式构造client
     */
    @Getter
    private String apiHost;
    /**
     * 自定义的okHttpClient
     * 如果不自定义 ，就是用sdk默认的OkHttpClient实例
     */
    @Getter
    private OkHttpClient okHttpClient;

    /**
     * api key的获取策略
     */
    @Getter
    private KeyStrategyFunction<List<String>, String> keyStrategy;

    @Getter
    private OpenAiApi openAiApi;

    /**
     * 自定义鉴权处理拦截器<br/>
     * 可以不设置，默认实现：DefaultOpenAiAuthInterceptor <br/>
     * 如需自定义实现参考：DealKeyWithOpenAiAuthInterceptor
     *
     * @see DynamicKeyOpenAiAuthInterceptor
     * @see DefaultOpenAiAuthInterceptor
     */
    @Getter
    private OpenAiAuthInterceptor authInterceptor;

    private static final String API_KEY = "sk-Waea254YSRYVg4FZVCz2CDz73B22xRpmKpJ41kbczVgpPxvg";

    HttpClient client = HttpClient.newHttpClient();

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
                .url(this.apiHost + "v1/chat/completions")
                .post(RequestBody.create(MediaType.parse(ContentType.JSON.getValue()), requestBody))
                .build();
            factory.newEventSource(request, eventSourceListener);
        } catch (Exception e) {
            log.error("请求参数解析异常：{}", e.getMessage());
        }
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
        System.out.println(totalUsage);
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
