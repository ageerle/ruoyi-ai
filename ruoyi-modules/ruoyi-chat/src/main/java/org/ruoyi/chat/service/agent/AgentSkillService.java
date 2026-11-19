package org.ruoyi.chat.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.skills.DefaultSkill;
import dev.langchain4j.skills.Skill;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.domain.AgentSkill;
import org.ruoyi.chat.domain.bo.agent.AgentSkillBo;
import org.ruoyi.chat.domain.vo.agent.SkillOptionVo;
import org.ruoyi.chat.mapper.AgentSkillMapper;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentSkillService {

    private static final String ENABLED = "0";

    private final AgentSkillMapper mapper;

    public TableDataInfo<AgentSkill> list(String name, String status, PageQuery pageQuery) {
        var query = Wrappers.<AgentSkill>lambdaQuery()
            .select(AgentSkill::getId, AgentSkill::getName, AgentSkill::getDescription,
                AgentSkill::getStatus, AgentSkill::getUpdateTime)
            .like(StringUtils.hasText(name), AgentSkill::getName, name)
            .eq(StringUtils.hasText(status), AgentSkill::getStatus, status)
            .orderByAsc(AgentSkill::getName);
        return TableDataInfo.build(mapper.selectPage(pageQuery.build(), query));
    }

    public AgentSkill get(Long id) {
        AgentSkill skill = mapper.selectById(id);
        if (skill == null) {
            throw new ServiceException("技能不存在");
        }
        return skill;
    }

    public boolean insert(AgentSkillBo bo) {
        AgentSkill skill = new AgentSkill();
        skill.setName(bo.getName());
        skill.setStatus(bo.getStatus() == null ? ENABLED : bo.getStatus());
        skill.setDescription(bo.getDescription().trim());
        skill.setContent(bo.getContent());
        return mapper.insert(skill) > 0;
    }

    public boolean update(AgentSkillBo bo) {
        AgentSkill existing = get(bo.getId());
        if (bo.getName() != null && !existing.getName().equals(bo.getName())) {
            throw new ServiceException("技能标识用于智能体关联，保存后不可修改");
        }
        AgentSkill update = new AgentSkill();
        update.setId(existing.getId());
        update.setDescription(bo.getDescription().trim());
        update.setContent(bo.getContent());
        update.setStatus(bo.getStatus());
        return mapper.updateById(update) > 0;
    }

    public boolean delete(Collection<Long> ids) {
        return !ids.isEmpty() && mapper.deleteByIds(ids) > 0;
    }

    public List<SkillOptionVo> listEnabledOptions() {
        return mapper.selectList(enabledQuery()
                .select(AgentSkill::getName, AgentSkill::getDescription)
                .orderByAsc(AgentSkill::getName))
            .stream()
            .map(skill -> new SkillOptionVo(skill.getName(), skill.getDescription()))
            .toList();
    }

    public void requireEnabled(List<String> names) {
        if (!names.isEmpty() && (names.stream().anyMatch(name -> !StringUtils.hasText(name))
            || mapper.selectCount(enabledQuery().in(AgentSkill::getName, names)) != names.size())) {
            throw new ServiceException("所选技能已停用或不存在，请刷新后重新选择");
        }
    }

    /** 每次对话按当前租户和智能体绑定读取，修改、停用或删除会在下一次对话生效。 */
    public List<Skill> resolveSkills(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> selected = names.stream().filter(StringUtils::hasText).distinct().toList();
        if (selected.isEmpty()) {
            return List.of();
        }
        var byName = mapper.selectList(enabledQuery().in(AgentSkill::getName, selected))
            .stream().collect(Collectors.toMap(AgentSkill::getName, Function.identity()));
        return selected.stream()
            .map(byName::get)
            .filter(Objects::nonNull)
            .<Skill>map(skill -> DefaultSkill.builder()
                .name(skill.getName()).description(skill.getDescription()).content(skill.getContent()).build())
            .toList();
    }

    private LambdaQueryWrapper<AgentSkill> enabledQuery() {
        return Wrappers.<AgentSkill>lambdaQuery().eq(AgentSkill::getStatus, ENABLED);
    }
}
