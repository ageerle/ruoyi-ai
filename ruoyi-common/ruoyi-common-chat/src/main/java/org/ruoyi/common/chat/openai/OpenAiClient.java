package org.ruoyi.common.chat.openai;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.reactivex.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.ruoyi.common.chat.constant.OpenAIConst;
import org.ruoyi.common.chat.entity.billing.BillingUsage;
import org.ruoyi.common.chat.entity.billing.Subscription;
import org.ruoyi.common.chat.entity.chat.*;
import org.ruoyi.common.chat.entity.common.DeleteResponse;
import org.ruoyi.common.chat.entity.common.OpenAiResponse;
import org.ruoyi.common.chat.entity.completions.Completion;
import org.ruoyi.common.chat.entity.completions.CompletionResponse;
import org.ruoyi.common.chat.entity.edits.Edit;
import org.ruoyi.common.chat.entity.edits.EditResponse;
import org.ruoyi.common.chat.entity.embeddings.Embedding;
import org.ruoyi.common.chat.entity.embeddings.EmbeddingResponse;
import org.ruoyi.common.chat.entity.engines.Engine;
import org.ruoyi.common.chat.entity.files.File;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.fineTune.Event;
import org.ruoyi.common.chat.entity.fineTune.FineTune;
import org.ruoyi.common.chat.entity.fineTune.FineTuneDeleteResponse;
import org.ruoyi.common.chat.entity.fineTune.FineTuneResponse;
import org.ruoyi.common.chat.entity.images.*;
import org.ruoyi.common.chat.entity.models.Model;
import org.ruoyi.common.chat.entity.models.ModelResponse;
import org.ruoyi.common.chat.entity.moderations.Moderation;
import org.ruoyi.common.chat.entity.moderations.ModerationResponse;
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
import org.ruoyi.common.core.exception.base.BaseException;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 *   open ai 客户端
 *
 * @author https:www.unfbx.com
 * @since 2023-02-11
 */

@Slf4j
public class OpenAiClient {
    /**
     * keys
     */
    @Getter
    @NotNull
    private List<String> apiKey;
    /**
     * 自定义api host使用builder的方式构造client
     */
    @Getter
    private String apiHost;
    @Getter
    private OpenAiApi openAiApi;
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

    /**
     * 构造器
     *
     * @return OpenAiClient.Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造
     *
     * @param builder
     */
    private OpenAiClient(Builder builder) {
        if (CollectionUtil.isEmpty(builder.apiKey)) {
            throw new BaseException(CommonError.API_KEYS_NOT_NUL.msg()
            );
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
     * 创建默认OkHttpClient
     *
     * @return
     */
    private OkHttpClient okHttpClient() {
        if (Objects.isNull(this.authInterceptor)) {
            this.authInterceptor = new DefaultOpenAiAuthInterceptor();
        }
        this.authInterceptor.setApiKey(this.apiKey);
        this.authInterceptor.setKeyStrategy(this.keyStrategy);
        return new OkHttpClient
                .Builder()
                .addInterceptor(this.authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build();
    }

    /**
     * openAi模型列表
     *
     * @return Model  list
     */
    public List<Model> models() {
        Single<ModelResponse> models = this.openAiApi.models();
        return models.blockingGet().getData();
    }

    /**
     * openAi模型详细信息
     *
     * @param id 模型主键
     * @return Model    模型类
     */
    public Model model(String id) {
        if (Objects.isNull(id) || "".equals(id)) {
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
        Single<Model> model = this.openAiApi.model(id);
        return model.blockingGet();
    }


    /**
     * 问答接口
     *
     * @param completion 问答参数
     * @return CompletionResponse
     */
    public CompletionResponse completions(Completion completion) {
        Single<CompletionResponse> completions = this.openAiApi.completions(completion);
        return completions.blockingGet();
    }

    /**
     * 问答接口-简易版
     *
     * @param question 问题描述
     * @return CompletionResponse
     */
    public CompletionResponse completions(String question) {
        Completion q = Completion.builder()
                .prompt(question)
                .build();
        Single<CompletionResponse> completions = this.openAiApi.completions(q);
        return completions.blockingGet();
    }

    /**
     * 文本修改
     *
     * @param edit 图片对象
     * @return EditResponse
     */
    public EditResponse edit(Edit edit) {
        Single<EditResponse> edits = this.openAiApi.edits(edit);
        return edits.blockingGet();
    }

    /**
     * 根据描述生成图片
     *
     * @param prompt 描述信息
     * @return ImageResponse
     */
    public ImageResponse genImages(String prompt) {
        Image image = Image.builder().prompt(prompt).build();
        return this.genImages(image);
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
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image  图片对象
     * @param prompt 描述信息
     * @return Item  list
     */
    public List<Item> editImages(java.io.File image, String prompt) {
        ImageEdit imageEdit = ImageEdit.builder().prompt(prompt).build();
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image     图片对象
     * @param imageEdit 图片参数
     * @return Item  list
     */
    public List<Item> editImages(java.io.File image, ImageEdit imageEdit) {
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image     png格式的图片，最大4MB
     * @param mask      png格式的图片，最大4MB
     * @param imageEdit 图片参数
     * @return Item list
     */
    public List<Item> editImages(java.io.File image, java.io.File mask, ImageEdit imageEdit) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        if (Objects.nonNull(mask)) {
            checkImageFormat(image);
            checkImageSize(image);
        }
        // 创建 RequestBody，用于封装构建RequestBody
        RequestBody imageBody = RequestBody.create(MediaType.parse("multipart/form-data"), image);
        MultipartBody.Part imageMultipartBody = MultipartBody.Part.createFormData("image", image.getName(), imageBody);
        MultipartBody.Part maskMultipartBody = null;
        if (Objects.nonNull(mask)) {
            RequestBody maskBody = RequestBody.create(MediaType.parse("multipart/form-data"), mask);
            maskMultipartBody = MultipartBody.Part.createFormData("mask", image.getName(), maskBody);
        }
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        requestBodyMap.put("prompt", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getPrompt()));
        requestBodyMap.put("n", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getN().toString()));
        requestBodyMap.put("size", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getSize()));
        requestBodyMap.put("response_format", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getResponseFormat()));
        if (!(Objects.isNull(imageEdit.getUser()) || "".equals(imageEdit.getUser()))) {
            requestBodyMap.put("user", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getUser()));
        }
        Single<ImageResponse> imageResponse = this.openAiApi.editImages(
                imageMultipartBody,
                maskMultipartBody,
                requestBodyMap
        );
        return imageResponse.blockingGet().getData();
    }

    /**
     * Creates a variation of a given image.
     * <p>
     * 变化图片，类似ai重做图片
     *
     * @param image           图片对象
     * @param imageVariations 图片参数
     * @return ImageResponse
     */
    public ImageResponse variationsImages(java.io.File image, ImageVariations imageVariations) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        RequestBody imageBody = RequestBody.create(MediaType.parse("multipart/form-data"), image);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("image", image.getName(), imageBody);
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        requestBodyMap.put("n", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getN().toString()));
        requestBodyMap.put("size", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getSize()));
        requestBodyMap.put("response_format", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getResponseFormat()));
        if (!(Objects.isNull(imageVariations.getUser()) || "".equals(imageVariations.getUser()))) {
            requestBodyMap.put("user", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getUser()));
        }
        Single<ImageResponse> variationsImages = this.openAiApi.variationsImages(
                multipartBody,
                requestBodyMap
        );
        return variationsImages.blockingGet();
    }

    /**
     * Creates a variation of a given image.
     *
     * @param image 图片对象
     * @return ImageResponse
     */
    public ImageResponse variationsImages(java.io.File image) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        ImageVariations imageVariations = ImageVariations.builder().build();
        return this.variationsImages(image, imageVariations);
    }

    /**
     * 校验图片不能为空
     *
     * @param image
     */
    private void checkImage(java.io.File image) {
        if (Objects.isNull(image)) {
            log.error("image不能为空");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
    }

    /**
     * 校验图片格式
     *
     * @param image
     */
    private void checkImageFormat(java.io.File image) {
        if (!(image.getName().endsWith("png") || image.getName().endsWith("PNG"))) {
            log.error("image格式错误");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
    }

    /**
     * 校验图片大小
     *
     * @param image
     */
    private void checkImageSize(java.io.File image) {
        if (image.length() > 4 * 1024 * 1024) {
            log.error("image最大支持4MB");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
    }

    /**
     * 向量计算：单文本
     *
     * @param input 单文本
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(String input) {
        List<String> inputs = new ArrayList<>(1);
        inputs.add(input);
        Embedding embedding = Embedding.builder().input(inputs).build();
        return this.embeddings(embedding);
    }

    /**
     * 向量计算：集合文本
     *
     * @param input 文本集合
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(List<String> input) {
        Embedding embedding = Embedding.builder().input(input).build();
        return this.embeddings(embedding);
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
     * 获取文件列表
     *
     * @return File  list
     */
    public List<File> files() {
        Single<OpenAiResponse<File>> files = this.openAiApi.files();
        return files.blockingGet().getData();
    }

    /**
     * 删除文件
     *
     * @param fileId 文件id
     * @return DeleteResponse
     */
    public DeleteResponse deleteFile(String fileId) {
        Single<DeleteResponse> deleteFile = this.openAiApi.deleteFile(fileId);
        return deleteFile.blockingGet();
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
     * 上传文件
     *
     * @param file 文件
     * @return UploadFileResponse
     */
    public UploadFileResponse uploadFile(java.io.File file) {
        //purpose 官网示例默认是：fine-tune
        return this.uploadFile("fine-tune", file);
    }

    /**
     * 检索文件
     *
     * @param fileId 文件id
     * @return File
     */
    public File retrieveFile(String fileId) {
        Single<File> fileContent = this.openAiApi.retrieveFile(fileId);
        return fileContent.blockingGet();
    }

    /**
     * 检索文件内容
     * 免费用户无法使用此接口 #未经过测试
     *
     * @param fileId
     * @return ResponseBody
     */
//    public ResponseBody retrieveFileContent(String fileId) {
//        Single<ResponseBody> fileContent = this.openAiApi.retrieveFileContent(fileId);
//        return fileContent.blockingGet();
//    }

    /**
     * 文本审核
     *
     * @param input 待检测数据
     * @return ModerationResponse
     */
    public ModerationResponse moderations(String input) {
        List<String> content = new ArrayList<>(1);
        content.add(input);
        Moderation moderation = Moderation.builder().input(content).build();
        return this.moderations(moderation);
    }

    /**
     * 文本审核
     *
     * @param input 待检测数据集合
     * @return ModerationResponse
     */
    public ModerationResponse moderations(List<String> input) {
        Moderation moderation = Moderation.builder().input(input).build();
        return this.moderations(moderation);
    }

    /**
     * 文本审核
     *
     * @param moderation 审核参数
     * @return ModerationResponse
     */
    public ModerationResponse moderations(Moderation moderation) {
        Single<ModerationResponse> moderations = this.openAiApi.moderations(moderation);
        return moderations.blockingGet();
    }

    /**
     * 创建微调模型
     *
     * @param fineTune 微调作业id
     * @return FineTuneResponse
     */
    public FineTuneResponse fineTune(FineTune fineTune) {
        Single<FineTuneResponse> fineTuneResponse = this.openAiApi.fineTune(fineTune);
        return fineTuneResponse.blockingGet();
    }

    /**
     * 创建微调模型
     *
     * @param trainingFileId 文件id，文件上传返回的id
     * @return FineTuneResponse
     */
    public FineTuneResponse fineTune(String trainingFileId) {
        FineTune fineTune = FineTune.builder().trainingFile(trainingFileId).build();
        return this.fineTune(fineTune);
    }

    /**
     * 微调模型列表
     *
     * @return FineTuneResponse list
     */
    public List<FineTuneResponse> fineTunes() {
        Single<OpenAiResponse<FineTuneResponse>> fineTunes = this.openAiApi.fineTunes();
        return fineTunes.blockingGet().getData();
    }

    /**
     * 检索微调作业
     *
     * @param fineTuneId 微调作业id
     * @return FineTuneResponse
     */
    public FineTuneResponse retrieveFineTune(String fineTuneId) {
        Single<FineTuneResponse> fineTune = this.openAiApi.retrieveFineTune(fineTuneId);
        return fineTune.blockingGet();
    }

    /**
     * 取消微调作业
     *
     * @param fineTuneId 主键
     * @return FineTuneResponse
     */
    public FineTuneResponse cancelFineTune(String fineTuneId) {
        Single<FineTuneResponse> fineTune = this.openAiApi.cancelFineTune(fineTuneId);
        return fineTune.blockingGet();
    }

    /**
     * 微调作业事件列表
     *
     * @param fineTuneId 微调作业id
     * @return Event List
     */
    public List<Event> fineTuneEvents(String fineTuneId) {
        Single<OpenAiResponse<Event>> events = this.openAiApi.fineTuneEvents(fineTuneId);
        return events.blockingGet().getData();
    }

    /**
     * 删除微调作业模型
     * Delete a fine-tuned model. You must have the Owner role in your organization.
     *
     * @param model 模型名称
     * @return FineTuneDeleteResponse
     */
    public FineTuneDeleteResponse deleteFineTuneModel(String model) {
        Single<FineTuneDeleteResponse> delete = this.openAiApi.deleteFineTuneModel(model);
        return delete.blockingGet();
    }


    /**
     * 引擎列表
     *
     * @return Engine List
     */
    @Deprecated
    public List<Engine> engines() {
        Single<OpenAiResponse<Engine>> engines = this.openAiApi.engines();
        return engines.blockingGet().getData();
    }

    /**
     * 引擎详细信息
     *
     * @param engineId 引擎id
     * @return Engine
     */
    @Deprecated
    public Engine engine(String engineId) {
        Single<Engine> engine = this.openAiApi.engine(engineId);
        return engine.blockingGet();
    }

    /**
     * 最新版的GPT-3.5 chat completion 更加贴近官方网站的问答模型
     *
     * @param chatCompletion 问答参数
     * @return 答案
     */
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) {
        Single<ChatCompletionResponse> chatCompletionResponse = this.openAiApi.chatCompletion(chatCompletion);
        return chatCompletionResponse.blockingGet();
    }

    /**
     * 简易版
     *
     * @param messages 问答参数
     * @return 答案
     */
    public ChatCompletionResponse chatCompletion(List<Message> messages) {
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).build();
        return this.chatCompletion(chatCompletion);
    }

    /**
     * 语音翻译：目前仅支持翻译为英文
     *
     * @param translations 参数
     * @param file         语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 翻译后文本
     */
    public WhisperResponse speechToTextTranslations(java.io.File file, Translations translations) {
        //文件
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
        //自定义参数
        Map<String, RequestBody> requestBodyMap = new HashMap<>(5,1L);

        if (StrUtil.isNotBlank(translations.getModel())) {
            requestBodyMap.put(Translations.Fields.model, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getModel()));
        }
        if (StrUtil.isNotBlank(translations.getPrompt())) {
            requestBodyMap.put(Translations.Fields.prompt, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getPrompt()));
        }
        if (StrUtil.isNotBlank(translations.getResponseFormat())) {
            requestBodyMap.put(Translations.Fields.responseFormat, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getResponseFormat()));
        }
        requestBodyMap.put(Translations.Fields.temperature, RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(translations.getTemperature())));
        Single<WhisperResponse> whisperResponse = this.openAiApi.speechToTextTranslations(multipartBody, requestBodyMap);
        return whisperResponse.blockingGet();
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
     * 简易版 语音翻译：目前仅支持翻译为英文
     *
     * @param file 语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 翻译后文本
     */
    public WhisperResponse speechToTextTranslations(java.io.File file) {
        Translations translations = Translations.builder().build();
        return this.speechToTextTranslations(file, translations);
    }

    /**
     * 校验语音文件大小给出提示，目前官方限制25MB，后续可能会改动所以不报错只做提示
     *
     * @param file
     */
    private void checkSpeechFileSize(java.io.File file) {
        if (file.length() > 25 * 1204 * 1024) {
            log.warn("2023-03-02官方文档提示：文件不能超出25MB");
        }
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


    public static final class Builder {
        /**
         * api keys
         */
        private @NotNull List<String> apiKey;
        /**
         * api请求地址，结尾处有斜杠
         *
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

        /**
         * @param val api请求地址，结尾处有斜杠
         * @return Builder对象
         */
        public Builder apiHost(String val) {
            apiHost = val;
            return this;
        }

        public Builder apiKey(@NotNull List<String> val) {
            apiKey = val;
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

        public OpenAiClient build() {
            return new OpenAiClient(this);
        }
    }
}
