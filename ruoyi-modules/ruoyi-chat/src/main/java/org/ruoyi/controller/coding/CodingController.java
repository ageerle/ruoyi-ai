package org.ruoyi.controller.coding;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.bo.coding.CodingRequestBo;
import org.ruoyi.enums.ModelType;
import org.ruoyi.service.coding.CodingWorkspaceService;
import org.ruoyi.service.coding.ICodingService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 编程能力接口（B 路径，不走 Supervisor 调度）
 *
 * <p>{@code /coding/**} 使用全局 Sa-Token 拦截器验证登录态。Controller 只做参数绑定
 * 与同步获取 userId（Sa-Token 异步上下文不传播），然后转发 Service。
 *
 * @author ageerle
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/coding")
@SaCheckPermission("coding:harness:use")
public class CodingController {

    private final ICodingService codingService;
    private final CodingWorkspaceService workspaceService;
    private final IChatModelService chatModelService;

    /**
     * The legacy endpoint exposes an unbounded AiServices loop with delete and process tools. It is
     * kept only as an explicit migration escape hatch; the durable Harness API is the safe default.
     */
    @Value("${coding.legacy.enabled:false}")
    private boolean legacyEnabled;

    /**
     * 编程对话（SSE 流式）
     *
     * @param bo 请求参数（prompt / model / workspacePath）
     * @return SseEmitter
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission({"coding:harness:write", "coding:harness:legacy-command"})
    public SseEmitter chat(@Valid @RequestBody CodingRequestBo bo) {
        requireLegacyEnabled();
        // 在进入异步执行前于已鉴权的 HTTP 线程取 userId。
        Long userId = LoginHelper.getUserId();
        return codingService.chat(bo, userId);
    }

    @GetMapping("/workspace")
    public R<CodingWorkspaceService.WorkspaceResult> workspace(
        @RequestParam(required = false) String workspacePath) throws Exception {
        requireLegacyEnabled();
        return R.ok(workspaceService.list(workspacePath));
    }

    @GetMapping("/models")
    public R<List<ModelOption>> models() {
        requireLegacyEnabled();
        // 编程对话只能用聊天模型，按 category=chat 过滤
        ChatModelBo bo = new ChatModelBo();
        bo.setCategory(ModelType.CHAT.getKey());
        List<ModelOption> models = chatModelService.queryAvailableList(bo).stream()
            .map(model -> new ModelOption(model.getId(), model.getModelName(), model.getProviderCode()))
            .toList();
        return R.ok(models);
    }

    @GetMapping("/file")
    public R<CodingWorkspaceService.FileContent> file(
        @RequestParam(required = false) String workspacePath,
        @RequestParam String path) throws Exception {
        requireLegacyEnabled();
        return R.ok(workspaceService.read(workspacePath, path));
    }

    @PutMapping("/file")
    @SaCheckPermission("coding:harness:write")
    public R<CodingWorkspaceService.FileContent> saveFile(@RequestBody FileWriteRequest request) throws Exception {
        requireLegacyEnabled();
        return R.ok(workspaceService.write(request.workspacePath(), request.path(), request.content()));
    }

    @PostMapping("/command")
    @SaCheckPermission({"coding:harness:write", "coding:harness:legacy-command"})
    public R<CodingWorkspaceService.CommandResult> command(@RequestBody CommandRequest request) {
        requireLegacyEnabled();
        return R.ok(workspaceService.execute(request.workspacePath(), request.command()));
    }

    private void requireLegacyEnabled() {
        if (!legacyEnabled) {
            throw new ResponseStatusException(HttpStatus.GONE,
                "Legacy coding endpoints are disabled; use /coding/harness");
        }
    }

    public record FileWriteRequest(String workspacePath, String path, String content) { }
    public record CommandRequest(String workspacePath, String command) { }
    public record ModelOption(Long id, String name, String provider) { }
}
