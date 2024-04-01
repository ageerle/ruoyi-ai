package com.xmzs.midjourney.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.midjourney.Constants;
import com.xmzs.midjourney.ProxyProperties;
import com.xmzs.midjourney.ReturnCode;
import com.xmzs.midjourney.dto.*;
import com.xmzs.midjourney.enums.TaskAction;
import com.xmzs.midjourney.enums.TaskStatus;
import com.xmzs.midjourney.enums.TranslateWay;
import com.xmzs.midjourney.exception.BannedPromptException;
import com.xmzs.midjourney.result.SubmitResultVO;
import com.xmzs.midjourney.service.TaskService;
import com.xmzs.midjourney.service.TaskStoreService;
import com.xmzs.midjourney.service.TranslateService;
import com.xmzs.midjourney.support.Task;
import com.xmzs.midjourney.support.TaskCondition;
import com.xmzs.midjourney.util.BannedPromptUtils;
import com.xmzs.midjourney.util.ConvertUtils;
import com.xmzs.midjourney.util.MimeTypeUtils;
import com.xmzs.midjourney.util.SnowFlake;
import com.xmzs.midjourney.util.TaskChangeParams;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.IChatService;
import com.xmzs.system.service.ISseService;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;

@Api(tags = "任务提交")
@RestController
@RequestMapping("/mj/submit")
@RequiredArgsConstructor
@Slf4j
public class SubmitController {
    private final TranslateService translateService;
    private final ProxyProperties properties;
    private final TaskService taskService;
    private final TaskStoreService taskStoreService;
    @Value("${chat.apiKey}")
    private String apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;
    @Autowired
    private IChatService chatService;
    @Autowired
    private IChatMessageService chatMessageService;
    @Autowired
    private ISseService sseService;

    @ApiOperation(value = "提交Imagine任务")
    @PostMapping("/imagine")
    public SubmitResultVO imagine(@RequestBody SubmitImagineDTO imagineDTO) {
        String prompt = imagineDTO.getPrompt();
        if (CharSequenceUtil.isBlank(prompt)) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "prompt不能为空");
        }
        prompt = prompt.trim();
        Task task = newTask(imagineDTO);
        task.setAction(TaskAction.IMAGINE);
        task.setPrompt(prompt);
        String promptEn = translatePrompt(prompt);
        try {
            BannedPromptUtils.checkBanned(promptEn);
        } catch (BannedPromptException e) {
            return SubmitResultVO.fail(ReturnCode.BANNED_PROMPT, "可能包含敏感词")
                .setProperty("promptEn", promptEn).setProperty("bannedWord", e.getMessage());
        }
        List<String> base64Array = Optional.ofNullable(imagineDTO.getBase64Array()).orElse(new ArrayList<>());
        if (CharSequenceUtil.isNotBlank(imagineDTO.getBase64())) {
            base64Array.add(imagineDTO.getBase64());
        }
        List<DataUrl> dataUrls;
        try {
            dataUrls = ConvertUtils.convertBase64Array(base64Array);
        } catch (MalformedURLException e) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
        }
        task.setPromptEn(promptEn);
        task.setDescription("/imagine " + prompt);
        return this.taskService.submitImagine(task, dataUrls);
    }

    @ApiOperation(value = "绘图变化-simple")
    @PostMapping("/simple-change")
    public SubmitResultVO simpleChange(@RequestBody SubmitSimpleChangeDTO simpleChangeDTO) {
        TaskChangeParams changeParams = ConvertUtils.convertChangeParams(simpleChangeDTO.getContent());
        if (changeParams == null) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "content参数错误");
        }
        SubmitChangeDTO changeDTO = new SubmitChangeDTO();
        changeDTO.setAction(changeParams.getAction());
        changeDTO.setTaskId(changeParams.getId());
        changeDTO.setIndex(changeParams.getIndex());
        changeDTO.setState(simpleChangeDTO.getState());
        changeDTO.setNotifyHook(simpleChangeDTO.getNotifyHook());
        return change(changeDTO);
    }

    @ApiOperation(value = "绘图变化")
    @PostMapping("/change")
    public SubmitResultVO change(@RequestBody SubmitChangeDTO changeDTO) {
        if (CharSequenceUtil.isBlank(changeDTO.getTaskId())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "taskId不能为空");
        }
        if (!Set.of(TaskAction.UPSCALE, TaskAction.VARIATION, TaskAction.REROLL).contains(changeDTO.getAction())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "action参数错误");
        }
        String description = "/up " + changeDTO.getTaskId();
        if (TaskAction.REROLL.equals(changeDTO.getAction())) {
            description += " R";
        } else {
            description += " " + changeDTO.getAction().name().charAt(0) + changeDTO.getIndex();
        }
        if (TaskAction.UPSCALE.equals(changeDTO.getAction())) {
            TaskCondition condition = new TaskCondition().setDescription(description);
            Task existTask = this.taskStoreService.findOne(condition);
            if (existTask != null) {
                return SubmitResultVO.of(ReturnCode.EXISTED, "任务已存在", existTask.getId())
                    .setProperty("status", existTask.getStatus())
                    .setProperty("imageUrl", existTask.getImageUrl());
            }
        }
        Task targetTask = this.taskStoreService.get(changeDTO.getTaskId());
        if (targetTask == null) {
            return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联任务不存在或已失效");
        }
        if (!TaskStatus.SUCCESS.equals(targetTask.getStatus())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务状态错误");
        }
        if (!Set.of(TaskAction.IMAGINE, TaskAction.VARIATION, TaskAction.REROLL, TaskAction.BLEND).contains(targetTask.getAction())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务不允许执行变化");
        }
        Task task = newTask(changeDTO);
        task.setAction(changeDTO.getAction());
        task.setPrompt(targetTask.getPrompt());
        task.setPromptEn(targetTask.getPromptEn());
        task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, targetTask.getProperty(Constants.TASK_PROPERTY_FINAL_PROMPT));
        task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, targetTask.getProperty(Constants.TASK_PROPERTY_MESSAGE_ID));
        task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, targetTask.getProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID));
        task.setDescription(description);
        int messageFlags = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_FLAGS);
        String messageId = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_MESSAGE_ID);
        String messageHash = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_MESSAGE_HASH);
        if (TaskAction.UPSCALE.equals(changeDTO.getAction())) {
            return this.taskService.submitUpscale(task, messageId, messageHash, changeDTO.getIndex(), messageFlags);
            } else if (TaskAction.VARIATION.equals(changeDTO.getAction())) {
            return this.taskService.submitVariation(task, messageId, messageHash, changeDTO.getIndex(), messageFlags);
        } else {
            return this.taskService.submitReroll(task, messageId, messageHash, messageFlags);
        }
    }

    @ApiOperation(value = "绘图变化")
    @PostMapping("/action")
    public String action(@RequestBody SubmitActionDTO changeDTO) {
        // 查询是否是付费用户
        sseService.checkUserGrade();
        // 扣除接口费用
        if ("upsample".equals(getAction(changeDTO.getCustomId()))) {
            mjTaskDeduct("放大", OpenAIConst.MJ_COST_TYPE2);
        } else {
            // Inpaint: 局部重绘
            // reroll 重绘
            // upsample 放大
            // zoom 变焦
            // upscale 高清放大
            // variation 变化
            if (!"Inpaint".equals(getAction(changeDTO.getCustomId()))) {
                mjTaskDeduct("变化", OpenAIConst.MJ_COST_TYPE1);
            }
        }
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 连接超时时间
            .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时时间
            .readTimeout(30, TimeUnit.SECONDS)    // 读取超时时间
            .build();

        String jsonStr = JSONUtil.toJsonStr(changeDTO);

        MediaType mediaType = MediaType.parse("application/json");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonStr, mediaType);
        Request request = new Request.Builder()
            .url(apiHost + "mj/submit/action")
            .method("POST", body)
            .header("mj-api-secret", apiKey) // 设置Authorization header
            .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            log.error("绘图变化失败:{}", e.getMessage());
        }
        return null;
    }

    public String getAction(String customId) {
        // 检查 customId 是否为空
        if(customId == null || customId.isEmpty()) {
            return null;
        }
        // 使用 "::" 分割字符串
        String[] parts = customId.split("::");
        // "MJ", "Inpaint", "1", "4fca7c14-181c-4...", "SOLO"
        if(customId.endsWith("SOLO")) {
            return parts[1];
        }
        // 返回 "upsample" 值，假设它总是在第三个位置
        return parts[2];
    }

    public void mjTaskDeduct(String prompt, double cost) {
        //扣除费用
        chatService.deductUserBalance(getUserId(), cost);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName("mj");
        chatMessageBo.setContent(prompt);
        chatMessageBo.setDeductCost(cost);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 获取用户Id
     *
     * @return
     */
    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }

    @ApiOperation(value = "提交Describe任务")
    @PostMapping("/describe")
    public SubmitResultVO describe(@RequestBody SubmitDescribeDTO describeDTO) {
        if (CharSequenceUtil.isBlank(describeDTO.getBase64())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64不能为空");
        }
        IDataUrlSerializer serializer = new DataUrlSerializer();
        DataUrl dataUrl;
        try {
            dataUrl = serializer.unserialize(describeDTO.getBase64());
        } catch (MalformedURLException e) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
        }
        Task task = newTask(describeDTO);
        task.setAction(TaskAction.DESCRIBE);
        String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
        task.setDescription("/describe " + taskFileName);
        return this.taskService.submitDescribe(task, dataUrl);
    }

    @ApiOperation(value = "提交Blend任务")
    @PostMapping("/blend")
    public SubmitResultVO blend(@RequestBody SubmitBlendDTO blendDTO) {
        List<String> base64Array = blendDTO.getBase64Array();
        if (base64Array == null || base64Array.size() < 2 || base64Array.size() > 5) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64List参数错误");
        }
        if (blendDTO.getDimensions() == null) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "dimensions参数错误");
        }
        IDataUrlSerializer serializer = new DataUrlSerializer();
        List<DataUrl> dataUrlList = new ArrayList<>();
        try {
            for (String base64 : base64Array) {
                DataUrl dataUrl = serializer.unserialize(base64);
                dataUrlList.add(dataUrl);
            }
        } catch (MalformedURLException e) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
        }
        Task task = newTask(blendDTO);
        task.setAction(TaskAction.BLEND);
        task.setDescription("/blend " + task.getId() + " " + dataUrlList.size());
        return this.taskService.submitBlend(task, dataUrlList, blendDTO.getDimensions());
    }

    private Task newTask(BaseSubmitDTO base) {
        Task task = new Task();
        task.setId(System.currentTimeMillis() + RandomUtil.randomNumbers(3));
        task.setSubmitTime(System.currentTimeMillis());
        task.setState(base.getState());
        String notifyHook = CharSequenceUtil.isBlank(base.getNotifyHook()) ? this.properties.getNotifyHook() : base.getNotifyHook();
        task.setProperty(Constants.TASK_PROPERTY_NOTIFY_HOOK, notifyHook);
        task.setProperty(Constants.TASK_PROPERTY_NONCE, SnowFlake.INSTANCE.nextId());
        return task;
    }

    private String translatePrompt(String prompt) {
        if (TranslateWay.NULL.equals(this.properties.getTranslateWay()) || CharSequenceUtil.isBlank(prompt)) {
            return prompt;
        }
        List<String> imageUrls = new ArrayList<>();
        Matcher imageMatcher = Pattern.compile("https?://[a-z0-9-_:@&?=+,.!/~*'%$]+\\x20+", Pattern.CASE_INSENSITIVE).matcher(prompt);
        while (imageMatcher.find()) {
            imageUrls.add(imageMatcher.group(0));
        }
        String paramStr = "";
        Matcher paramMatcher = Pattern.compile("\\x20+-{1,2}[a-z]+.*$", Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (paramMatcher.find()) {
            paramStr = paramMatcher.group(0);
        }
        String imageStr = CharSequenceUtil.join("", imageUrls);
        String text = prompt.substring(imageStr.length(), prompt.length() - paramStr.length());
        if (CharSequenceUtil.isNotBlank(text)) {
            text = this.translateService.translateToEnglish(text).trim();
        }
        return imageStr + text + paramStr;
    }

}
