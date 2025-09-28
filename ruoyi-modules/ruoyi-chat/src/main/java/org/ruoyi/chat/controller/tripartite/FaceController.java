package org.ruoyi.chat.controller.tripartite;

import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.ruoyi.chat.domain.InsightFace;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.util.MjOkHttpUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 绘画(换脸)任务查询
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Tag(name = "任务查询")
@RestController
@RequestMapping("/mj")
@RequiredArgsConstructor
@Slf4j
public class FaceController {

    private final IChatCostService chatCostService;

    private final MjOkHttpUtil mjOkHttpUtil;

    @Operation(summary = "换脸")
    @PostMapping("/insight-face/swap")
    public String insightFace(@RequestBody InsightFace insightFace) {
        // 扣除接口费用并且保存消息记录
        chatCostService.taskDeduct("mj","Face Changing", 0.0);
        // 创建请求体（这里使用JSON作为媒体类型）
        String insightFaceJson = JSONUtil.toJsonStr(insightFace);
        String url = "mj/insight-face/swap";
        Request request = mjOkHttpUtil.createPostRequest(url, insightFaceJson);
        return mjOkHttpUtil.executeRequest(request);
    }

}
