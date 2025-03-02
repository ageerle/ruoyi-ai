package org.ruoyi.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.cofing.OkHttpConfig;
import org.ruoyi.system.domain.vo.ppt.*;
import org.ruoyi.system.service.IChatCostService;
import org.ruoyi.system.service.IPptService;
import org.ruoyi.system.util.HttpUtils;
import org.ruoyi.system.util.WddPptApi;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.time.Duration;

import static org.ruoyi.common.satoken.utils.LoginHelper.getLoginUser;

/**
 * AI-PPTService业务层处理
 *
 * @author NSL
 * @since 2024-12-30
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PptServiceImpl implements IPptService {

    private final OkHttpConfig okHttpConfig;
    private final ConfigService configService;
    private final IChatCostService chatCostService;

    @Override
    public void syncStreamGeneratePpt(String title) {
        // 创建 api token (有效期2小时，同一个 uid 创建时之前的 token 会在10秒内失效)
        String apiToken = getApiToken();
        // 生成大纲
        System.out.println("\n\n========== 正在生成大纲 ==========");
        String outline = WddPptApi.generateOutline(apiToken, title, null, null);

        // 生成大纲内容
        System.out.println("\n\n========== 正在生成大纲内容 ==========");
        String markdown = WddPptApi.generateContent(apiToken, outline, null, null);

        // 随机一个模板
        System.out.println("\n\n========== 随机选择模板 ==========");
        String templateId = WddPptApi.randomOneTemplateId(apiToken);
        System.out.println(templateId);

        // 生成PPT
        System.out.println("\n\n========== 正在生成PPT ==========");
        JSONObject pptInfo = WddPptApi.generatePptx(apiToken, templateId, markdown, false);
        String pptId = pptInfo.getString("id");
        System.out.println("pptId: " + pptId);
        System.out.println("ppt主题：" + pptInfo.getString("subject"));
        System.out.println("ppt封面：" + pptInfo.getString("coverUrl") + "?token=" + apiToken);

        // 下载PPT到桌面
        System.out.println("\n\n========== 正在下载PPT ==========");
        JSONObject result = WddPptApi.downloadPptx(apiToken, pptId);
        String url = result.getString("fileUrl");
        System.out.println("ppt链接：" + url);
        String savePath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator + pptId + ".pptx";
        try {
            HttpUtils.download(url, new File(savePath));
        } catch (Exception e) {

        }
        System.out.println("ppt下载完成，保存路径：" + savePath);
    }

    @Override
    public void selectPptList(PptAllQueryDto pptQueryVo) {
        String apiToken = getApiToken();
        // todo 无权限访问
        JSONObject jsonObject = WddPptApi.listAllPptx(apiToken, JSONUtil.toJsonStr(pptQueryVo));
        System.out.println(jsonObject);
    }

    @Override
    public JSONObject getPptTemplates(PptTemplateQueryDto pptQueryVo) {
        return WddPptApi.getPptTemplates(getApiToken(), JSONUtil.toJsonStr(pptQueryVo));
    }

    @Override
    public SseEmitter generateOutline(PptGenerateOutlineDto generateOutlineDto) {
        return WddPptApi.sseGenerateOutline(getApiToken(), generateOutlineDto.getSubject(), generateOutlineDto.getDataUrl(), generateOutlineDto.getPrompt());
    }

    @Override
    public SseEmitter generateContent(PptGenerateContentDto generateContentDto) {
        return WddPptApi.sseGenerateContent(getApiToken(), generateContentDto.getOutlineMarkdown(), generateContentDto.getDataUrl(), generateContentDto.getPrompt());
    }

    @Override
    public JSONObject generatePptx(PptGeneratePptxDto generatePptxDto) {
        return WddPptApi.generatePptx(getApiToken(), generatePptxDto.getTemplateId(), generatePptxDto.getOutlineContentMarkdown(), generatePptxDto.isPptxProperty());
    }

    public String getKey(String key) {
        return configService.getConfigValue("ppt", key);
    }

    @Override
    public String getApiToken() {
        LoginUser loginUser = null;
        try {
            loginUser = LoginHelper.getLoginUser();
        } catch (Exception e) {
            throw new NotLoginException(NotLoginException.NOT_TOKEN_MESSAGE, NotLoginException.NOT_TOKEN, NotLoginException.NOT_TOKEN);
        }
        String userId = String.valueOf(loginUser.getUserId());
        // 创建 api token (有效期2小时，同一个 uid 创建时之前的 token 会在10秒内失效)
        String apiToken = RedisUtils.getCacheObject(Constants.WDD_TOKEN_KEY + userId);
        if (StrUtil.isEmpty(apiToken)) {
            apiToken = WddPptApi.createApiToken(getKey("apiKey"), userId, null);
            RedisUtils.setCacheObject(Constants.WDD_TOKEN_KEY, apiToken, Duration.ofMinutes(Constants.WDD_TOKEN_EXPIRATION));
        }
        return apiToken;
    }

    @Override
    public void successCallback() {
        // 扣费
        chatCostService.taskDeduct("ppt", "AI-PPT", NumberUtils.toDouble(okHttpConfig.getGenerate(), 1.1));
    }
}
