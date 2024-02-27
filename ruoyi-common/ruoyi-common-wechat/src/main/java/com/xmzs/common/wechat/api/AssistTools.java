package com.xmzs.common.wechat.api;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 辅助工具类，该类暂时未用，请忽略
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年5月22日 下午10:34:46
 * @version 1.0
 *
 */
public class AssistTools {
	private static OkHttpClient client = new OkHttpClient();
	private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

	public static boolean sendQrPicToServer(String username, String password, String uploadUrl, String localPath)
			throws IOException {
		File file = new File(localPath);
		RequestBody requestBody = new MultipartBody.Builder().addFormDataPart("username", username)
				.addFormDataPart("password", password)
				.addFormDataPart("file", file.getName(), RequestBody.create(MEDIA_TYPE_PNG, file)).build();
		Request request = new Request.Builder().url(uploadUrl).post(requestBody).build();
		Call call = client.newCall(request);
		try {
			Response response = call.execute();
			System.out.println(response.body().string());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

}
