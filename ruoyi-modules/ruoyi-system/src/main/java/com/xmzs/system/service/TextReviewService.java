package com.xmzs.system.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URLEncoder;


/**
 * 文本审核服务
 *
 * @author: wangle
 * @date: 2023/5/27
 */
//@Service
@Slf4j
public class TextReviewService {
    @Value("${baidu.textReview.apiKey}")
    private String API_KEY;
    @Value("${baidu.textReview.secretKey}")
    private String SECRET_KEY;

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    /**
     * 文本内容审核
     *
     * @param msg
     * @return String
     * @Date 2023/5/27
     **/
    public String textReview(String msg) {
        String conclusionType = "";
        try {
            String text = URLEncoder.encode(msg);
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, "text=" + text);
            Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined?access_token=" + getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            JSONObject jsonObject = JSONObject.parseObject(response.body().string());
            conclusionType = jsonObject.getString("conclusionType");
        } catch (IOException e) {
            log.info("发生错误{}", e.getMessage());
        }
        return conclusionType;
    }

    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    public String getAccessToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY
            + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
            .url("https://aip.baidubce.com/oauth/2.0/token")
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return JSONObject.parseObject(response.body().string()).getString("access_token");
    }
}
