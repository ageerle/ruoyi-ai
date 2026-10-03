package org.ruoyi.controller.shortdrama;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.service.shortdrama.impl.ShortDramaSkillCatalog;
import org.ruoyi.service.shortdrama.impl.ShortDramaSkillMarket;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/skills")
public class ShortDramaSkillController {
    private final ShortDramaSkillCatalog catalog;
    private final ShortDramaSkillMarket market;
    @GetMapping("/market") public R<List<ShortDramaSkillMarket.Entry>> market() { return R.ok(market.list()); }
    @GetMapping public R<List<ShortDramaSkillCatalog.Option>> list(@RequestParam(required=false) String type,
        @RequestParam(defaultValue="false") boolean includeDisabled) { return R.ok(catalog.list(type, includeDisabled)); }
    @GetMapping("/{name}") public R<ShortDramaSkillCatalog.Skill> detail(@PathVariable String name) { return R.ok(catalog.detail(name)); }
    @PostMapping @SaCheckPermission("shortDrama:skill:manage")
    public R<ShortDramaSkillCatalog.Skill> create(@RequestBody ShortDramaSkillCatalog.Save body) { return R.ok(catalog.save(body.name(), body, true)); }
    @PutMapping("/{name}") @SaCheckPermission("shortDrama:skill:manage")
    public R<ShortDramaSkillCatalog.Skill> update(@PathVariable String name, @RequestBody ShortDramaSkillCatalog.Save body) { return R.ok(catalog.save(name, body, false)); }
}
