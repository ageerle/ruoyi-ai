package org.ruoyi.common.chat.localModels;



import org.ruoyi.common.chat.entity.models.LocalModelsSearchRequest;
import org.ruoyi.common.chat.entity.models.LocalModelsSearchResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
/**
 * @program: RUOYIAI
 * @ClassName SearchService
 * @description: 请求模型
 * @author: hejh
 * @create: 2025-03-15 17:27
 * @Version 1.0
 **/


public interface SearchService {
    @POST("/vectorize") // 与 Flask 服务中的路由匹配
    Call<LocalModelsSearchResponse> vectorize(@Body LocalModelsSearchRequest request);
}


