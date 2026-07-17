package org.ruoyi.controller.coding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.bo.coding.CodingRequestBo;
import org.ruoyi.service.coding.CodingWorkspaceService;
import org.ruoyi.service.coding.ICodingService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 编程能力接口（B 路径，不走 Supervisor 调度）
 *
 * <p>第一阶段 {@code /coding/**} 在 {@code application.yml} 的 security.excludes 中，
 * 免鉴权直连。Controller 只做参数绑定 + 同步取 userId（Sa-Token 异步上下文丢失，
 * 见 SecurityConfig 注释）+ 转发 Service。
 *
 * @author ageerle
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/coding")
public class CodingController {

    private final ICodingService codingService;
    private final CodingWorkspaceService workspaceService;
    private final IChatModelService chatModelService;

    /**
     * 编程对话（SSE 流式）
     *
     * @param bo 请求参数（prompt / model / workspacePath）
     * @return SseEmitter
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody CodingRequestBo bo) {
        // 同步线程取 userId；第一阶段免鉴权，可能为 null
        Long userId = LoginHelper.getUserId();
        return codingService.chat(bo, userId);
    }

    @GetMapping("/workspace")
    public R<CodingWorkspaceService.WorkspaceResult> workspace(
        @RequestParam(required = false) String workspacePath) throws Exception {
        return R.ok(workspaceService.list(workspacePath));
    }

    @GetMapping("/models")
    public R<List<ModelOption>> models() {
        List<ModelOption> models = chatModelService.queryList(new ChatModelBo()).stream()
            .filter(model -> "1".equals(model.getModelShow()))
            .map(model -> new ModelOption(model.getId(), model.getModelName(), model.getProviderCode()))
            .toList();
        return R.ok(models);
    }

    @GetMapping("/file")
    public R<CodingWorkspaceService.FileContent> file(
        @RequestParam(required = false) String workspacePath,
        @RequestParam String path) throws Exception {
        return R.ok(workspaceService.read(workspacePath, path));
    }

    @PutMapping("/file")
    public R<CodingWorkspaceService.FileContent> saveFile(@RequestBody FileWriteRequest request) throws Exception {
        return R.ok(workspaceService.write(request.workspacePath(), request.path(), request.content()));
    }

    @PostMapping("/command")
    public R<CodingWorkspaceService.CommandResult> command(@RequestBody CommandRequest request) {
        return R.ok(workspaceService.execute(request.workspacePath(), request.command()));
    }

    public record FileWriteRequest(String workspacePath, String path, String content) { }
    public record CommandRequest(String workspacePath, String command) { }
    public record ModelOption(Long id, String name, String provider) { }
}
