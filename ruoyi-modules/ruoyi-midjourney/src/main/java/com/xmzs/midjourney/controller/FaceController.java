package com.xmzs.midjourney.controller;

import cn.hutool.json.JSONUtil;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.midjourney.domain.InsightFace;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.IChatService;
import com.xmzs.system.service.ISseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/mj")
@RequiredArgsConstructor
@Slf4j
public class FaceController {

    @Value("${chat.apiKey}")
    private String apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;

    @Autowired
    private IChatService chatService;

    @Autowired
    private ISseService sseService;

    @ApiOperation(value = "换脸")
    @PostMapping("/insight-face/swap")
    public String insightFace(@RequestBody InsightFace insightFace) {
        // 查询是否是付费用户
        sseService.checkUserGrade();
        // 扣除接口费用
        chatService.mjTaskDeduct("换脸", OpenAIConst.MJ_COST_TYPE2);
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 连接超时时间
            .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时时间
            .readTimeout(30, TimeUnit.SECONDS)    // 读取超时时间
            .build();
        // 创建一个Request对象来配置你的请求
        // 创建请求体（这里使用JSON作为媒体类型）
        String jsonStr = JSONUtil.toJsonStr(insightFace);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonStr, JSON);
        Buffer buffer = new Buffer();
        GzipSink gzipSink = new GzipSink(buffer);
        BufferedSink gzipBufferedSink = Okio.buffer(gzipSink);
        try {
            body.writeTo(gzipBufferedSink);
            gzipBufferedSink.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 创建POST请求
        Request request = new Request.Builder()
            .header("mj-api-secret", apiKey)
            .header("Content-Encoding", "gzip")
            .url(apiHost + "mj/insight-face/swap") // 替换为你的URL
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            log.error("换脸失败! {}", e.getMessage());
        }
        return null;
    }

}
