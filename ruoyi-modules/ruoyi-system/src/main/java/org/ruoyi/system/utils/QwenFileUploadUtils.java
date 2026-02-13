package org.ruoyi.system.utils;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.ruoyi.common.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.rmi.ServerException;

/***
 * 千问上传文件工具类
 */
public class QwenFileUploadUtils {

    // 上传本地文件
    public static String uploadFile(File file, String apiHost, String apiKey) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // 构建 multipart/form-data 请求体（千问要求的格式）
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(), // 参数名必须为 file
                RequestBody.create(MediaType.parse("application/octet-stream"), file))
            .addFormDataPart("purpose", "file-extract") // 必须为 file-extract，文档解析专用
            .build();

        // 构建请求（必须为 POST 方法）
        Request request = new Request.Builder()
            .url(apiHost)
            .post(requestBody)
            .addHeader("Authorization", apiKey) // 认证头格式正确
            .build();

        // 发送请求并解析 fileId
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServerException("上传失败：" + response.code() + " " + response.message());
            }
            // 解析响应体，获取 fileId
            String responseBody = response.body().string();
            if (StringUtils.isEmpty(responseBody)){
                throw new ServerException("上传失败：响应体为空");
            }
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            // 千问返回的 fileId
            return jsonObject.getString("id");
        }
    }
}
