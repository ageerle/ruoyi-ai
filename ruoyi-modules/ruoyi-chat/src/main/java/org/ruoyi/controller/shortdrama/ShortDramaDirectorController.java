package org.ruoyi.controller.shortdrama;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaDirectorService;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}/director/{id}")
public class ShortDramaDirectorController {
    private final ShortDramaDirectorService service;
    @GetMapping public R<ShortDramaDirectorService.Director> read(@PathVariable Long projectId, @PathVariable String id) throws Exception {
        return R.ok(service.read(projectId, id, LoginHelper.getUserId()));
    }
    @PutMapping public R<ShortDramaDirectorService.Director> save(@PathVariable Long projectId, @PathVariable String id, @RequestBody ShortDramaDirectorService.Director request) throws Exception {
        return R.ok(service.save(projectId, id, LoginHelper.getUserId(), request));
    }
}
