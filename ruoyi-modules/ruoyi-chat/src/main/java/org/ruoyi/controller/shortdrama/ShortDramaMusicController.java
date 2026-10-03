package org.ruoyi.controller.shortdrama;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaMusicService;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}/music")
public class ShortDramaMusicController {
    private final ShortDramaMusicService service;
    @GetMapping public R<?> list(@PathVariable Long projectId) throws Exception {return R.ok(service.list(projectId,LoginHelper.getUserId()));}
    @PostMapping public R<?> generate(@PathVariable Long projectId,@RequestBody ShortDramaMusicService.MusicRequest request) throws Exception {return R.ok(service.generate(projectId,LoginHelper.getUserId(),request));}
    @GetMapping("/{id}") public R<?> poll(@PathVariable Long projectId,@PathVariable String id) throws Exception {return R.ok(service.poll(projectId,LoginHelper.getUserId(),id));}
    @PostMapping("/{id}/use") public R<?> use(@PathVariable Long projectId,@PathVariable String id,@RequestBody ShortDramaMusicService.Usage usage) throws Exception {return R.ok(service.use(projectId,LoginHelper.getUserId(),id,usage));}
    @PostMapping("/write") public R<?> write(@PathVariable Long projectId,@RequestBody ShortDramaMusicService.BriefRequest request) {return R.ok(service.write(projectId,LoginHelper.getUserId(),request));}
}
