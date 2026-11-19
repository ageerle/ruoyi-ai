package org.ruoyi.chat.controller.agent;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.domain.AgentSkill;
import org.ruoyi.chat.domain.bo.agent.AgentSkillBo;
import org.ruoyi.chat.service.agent.AgentSkillService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 技能管理。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent/skill")
public class AgentSkillController extends BaseController {

    private final AgentSkillService skillService;

    @SaCheckPermission("agent:skill:list")
    @GetMapping("/list")
    public TableDataInfo<AgentSkill> list(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) String status, PageQuery pageQuery) {
        return skillService.list(name, status, pageQuery);
    }

    @SaCheckPermission("agent:skill:query")
    @GetMapping("/{id}")
    public R<AgentSkill> get(@PathVariable Long id) {
        return R.ok(skillService.get(id));
    }

    @SaCheckPermission("agent:skill:add")
    @Log(title = "技能管理", businessType = BusinessType.INSERT, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AgentSkillBo bo) {
        return toAjax(skillService.insert(bo));
    }

    @SaCheckPermission("agent:skill:edit")
    @Log(title = "技能管理", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AgentSkillBo bo) {
        return toAjax(skillService.update(bo));
    }

    @SaCheckPermission("agent:skill:remove")
    @Log(title = "技能管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable Long[] ids) {
        return toAjax(skillService.delete(List.of(ids)));
    }
}
