package org.ruoyi.controller.shortdrama;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaCanvasService;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}/canvas")
public class ShortDramaCanvasController {
    private final ShortDramaCanvasService service;
    @GetMapping public R<ShortDramaCanvasService.Canvas> read(@PathVariable Long projectId) throws Exception {
        return R.ok(service.read(projectId, LoginHelper.getUserId()));
    }
    @PutMapping public R<ShortDramaCanvasService.Canvas> save(@PathVariable Long projectId, @RequestBody ShortDramaCanvasService.Canvas request) throws Exception {
        return R.ok(service.save(projectId, LoginHelper.getUserId(), request));
    }
}
