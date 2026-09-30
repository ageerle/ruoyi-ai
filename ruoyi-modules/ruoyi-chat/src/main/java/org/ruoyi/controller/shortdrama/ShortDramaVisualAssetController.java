package org.ruoyi.controller.shortdrama;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaVisualAssetService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}/visual-assets")
public class ShortDramaVisualAssetController {
    private final ShortDramaVisualAssetService service;
    public record ReviseFrame(String prompt,String model) { }
    @PostMapping("/{assetId}/revise") public R<Map<String,Object>> revise(@PathVariable Long projectId,@PathVariable Long assetId,@RequestBody ReviseFrame body) {return R.ok(service.reviseFrame(projectId,assetId,body.prompt(),body.model(),LoginHelper.getUserId()));}
    public record Prop(String title,String prompt,java.util.List<Integer> shotNumbers,String imageUrl) { }
    @PostMapping("/prop") public R<Map<String,Object>> prop(@PathVariable Long projectId,@RequestBody Prop body) {return R.ok(service.saveProp(projectId,body.title(),body.prompt(),body.shotNumbers(),body.imageUrl(),LoginHelper.getUserId()));}
    @PostMapping("/generate-opening") public R<Map<String,Object>> opening(@PathVariable Long projectId,@RequestParam String model) {return R.ok(service.generateOpening(projectId,model,LoginHelper.getUserId()));}
    @PostMapping("/generate-range") public R<Map<String,Object>> range(@PathVariable Long projectId,@RequestParam String model,@RequestParam int start,@RequestParam int end) {return R.ok(service.generateRange(projectId,model,LoginHelper.getUserId(),start,end));}
    @GetMapping public R<Map<String,Object>> status(@PathVariable Long projectId) {return R.ok(service.status(projectId,LoginHelper.getUserId()));}
    @PostMapping("/plan") public R<Map<String,Object>> plan(@PathVariable Long projectId,@RequestParam(required=false) String model) {return R.ok(service.plan(projectId,model,LoginHelper.getUserId()));}
    @PostMapping("/recover") public R<Map<String,Object>> recover(@PathVariable Long projectId) {return R.ok(service.recover(projectId,LoginHelper.getUserId()));}
    @PostMapping("/generate") public R<Map<String,Object>> generate(@PathVariable Long projectId,@RequestParam String model) {return R.ok(service.generate(projectId,model,LoginHelper.getUserId()));}
}
